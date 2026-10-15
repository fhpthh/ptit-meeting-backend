package org.ptit.meeting.modules.timetable.facade.impl;

import java.io.InputStream;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ptit.meeting.common.service.FileStorageService;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.adapter.ExcelAdapterFactory;
import org.ptit.meeting.modules.timetable.adapter.ExcelImportAdapter;
import org.ptit.meeting.modules.timetable.constant.TimetableConstants;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportResultResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.ptit.meeting.modules.timetable.facade.TimetableAdminFacade;
import org.ptit.meeting.modules.timetable.repository.ImportHistoryRepository;
import org.ptit.meeting.modules.timetable.service.TimetableService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableAdminFacadeImpl implements TimetableAdminFacade {

  private final FileStorageService fileStorageService;
  private final ImportHistoryRepository historyRepository;
  private final ExcelAdapterFactory adapterFactory;
  private final TimetableService timetableService;
  private final ObjectMapper objectMapper;

  @Override
  public ImportResultResponse importMasterTimetable(Long semesterId, MultipartFile file, Long uploadedBy) {
    // 1. Kiểm tra tiến trình import trùng lặp trong cùng học kỳ
    checkConcurrentImport(semesterId);

    // 2. Lưu file vật lý đối soát
    String filePath = fileStorageService.storeFile(file, TimetableConstants.MODULE_NAME);

    // 3. Khởi tạo bản ghi lịch sử
    ImportHistory history = initHistory(semesterId, uploadedBy, file.getOriginalFilename(), filePath);

    List<ImportRowError> errors = new ArrayList<>();
    try {
      // 4. Mở file và dùng Adapter đọc dữ liệu
      ExcelImportAdapter<MasterTimetableRowDto> adapter = adapterFactory.getAdapter(ImportType.MASTER_TIMETABLE);
      List<MasterTimetableRowDto> validRows = parseExcel(file, adapter, errors);

      // 5. Bàn giao danh sách sạch cho TimetableService lưu CSDL
      int successCount = timetableService.processMasterTimetable(semesterId, validRows, errors);

      // 6. Cập nhật kết quả cuối cùng
      int failedCount = (validRows.size() + errors.size()) - successCount;
      updateHistorySuccess(history, validRows.size(), successCount, failedCount, errors);

      return ImportResultResponse.builder()
          .totalRows(validRows.size())
          .successRows(successCount)
          .failureRows(failedCount)
          .errors(errors)
          .build();

    } catch (Exception e) {
      log.error("(importMasterTimetable) Lỗi hệ thống: ", e);
      updateHistoryFailed(history, e.getMessage());
      throw e;
    }
  }

  private void checkConcurrentImport(Long semesterId) {
    historyRepository.findFirstBySemesterIdAndStatusOrderByCreatedAtDesc(semesterId, ImportStatus.PROCESSING)
        .ifPresent(h -> {
          // Nếu tiến trình import cũ đã chạy quá 3 phút (thường do server crash hoặc restart giữa chừng)
          if (h.getCreatedAt() != null && h.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusMinutes(3))) {
            log.warn("(checkConcurrentImport) Phát hiện tiến trình cũ id={} bị gián đoạn/treo quá 3 phút, tự động chuyển FAILED", h.getId());
            h.setStatus(ImportStatus.FAILED);
            try {
              h.setErrorLog(objectMapper.writeValueAsString(java.util.Map.of("reason", "Tiến trình import bị gián đoạn do khởi động lại hệ thống hoặc timeout")));
            } catch (Exception ignored) {
              h.setErrorLog("{\"reason\": \"Tiến trình import bị gián đoạn\"}");
            }
            historyRepository.save(h);
            return;
          }
          throw BusinessException.invalidInput(TimetableErrorConstants.IMPORT_IN_PROGRESS);
        });
  }

  private ImportHistory initHistory(Long semesterId, Long uploadedBy, String fileName, String filePath) {
    return historyRepository.save(
        ImportHistory.builder()
            .semesterId(semesterId)
            .uploadedBy(uploadedBy)
            .fileName(fileName)
            .fileUrl(filePath)
            .importType(ImportType.MASTER_TIMETABLE)
            .status(ImportStatus.PROCESSING)
            .build()
    );
  }

  private List<MasterTimetableRowDto> parseExcel(
      MultipartFile file,
      ExcelImportAdapter<MasterTimetableRowDto> adapter,
      List<ImportRowError> errors
  ) {
    List<MasterTimetableRowDto> validRows = new ArrayList<>();
    try (InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      adapter.initializeHeader(sheet);

      int startRow = adapter.getDataStartRow();
      for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        int rowNumber = i + 1;

        MasterTimetableRowDto dto = adapter.adapt(row, rowNumber, errors);
        if (dto != null && adapter.validate(dto, rowNumber, errors)) {
          validRows.add(dto);
        }
      }
    } catch (BusinessException be) {
      log.error("(parseExcel) Lỗi nghiệp vụ cấu trúc Excel: ", be);
      throw be;
    } catch (Exception e) {
      log.error("(parseExcel) Lỗi đọc hoặc phân tích file Excel: ", e);
      throw BusinessException.invalidInputFile(TimetableErrorConstants.FILE_INVALID_FORMAT);
    }
    return validRows;
  }

  private void updateHistorySuccess(ImportHistory history, int total, int success, int failed, List<ImportRowError> errors) {
    history.setTotalRows(total);
    history.setSuccessRows(success);
    history.setFailedRows(failed);
    history.setStatus(failed == 0 ? ImportStatus.SUCCESS : ImportStatus.PARTIAL_SUCCESS);
    try {
      history.setErrorLog(errors.isEmpty() ? null : objectMapper.writeValueAsString(errors));
    } catch (Exception e) {
      log.warn("(updateHistorySuccess) Lỗi serialize error_log", e);
      history.setErrorLog(null);
    }
    historyRepository.save(history);
  }

  private void updateHistoryFailed(ImportHistory history, String error) {
    history.setStatus(ImportStatus.FAILED);
    try {
      history.setErrorLog(objectMapper.writeValueAsString(java.util.Map.of("error", error != null ? error : "Unknown error")));
    } catch (Exception e) {
      log.warn("(updateHistoryFailed) Lỗi serialize error_log", e);
      history.setErrorLog("{\"error\": \"Lỗi hệ thống không xác định\"}");
    }
    historyRepository.save(history);
  }
}