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
import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
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
  private final SemesterRepository semesterRepository;
  private final ExcelAdapterFactory adapterFactory;
  private final TimetableService timetableService;
  private final ObjectMapper objectMapper;

  @Override
  public ImportResultResponse importMasterTimetable(Long semesterId, MultipartFile file, Long uploadedBy) {
    // 1. Kiểm tra học kỳ tồn tại (Fail fast)
    Semester semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.SEMESTER_NOT_FOUND));

    // 2. Kiểm tra tiến trình import trùng lặp trong cùng học kỳ
    checkConcurrentImport(semesterId);

    // 3. Lưu file vật lý đối soát
    String filePath = fileStorageService.storeFile(file, TimetableConstants.MODULE_NAME);

    // 4. Khởi tạo bản ghi lịch sử
    ImportHistory history = initHistory(semesterId, uploadedBy, file.getOriginalFilename(), filePath);

    List<ImportRowError> errors = new ArrayList<>();
    try {
      // 5. Mở file và dùng Adapter đọc dữ liệu
      ExcelImportAdapter<MasterTimetableRowDto> adapter = adapterFactory.getAdapter(ImportType.MASTER_TIMETABLE);
      List<MasterTimetableRowDto> validRows = parseExcel(file, adapter, errors);

      // 6. Bàn giao danh sách sạch cho TimetableService lưu CSDL
      TimetableProcessResult result = timetableService.processMasterTimetable(semesterId, validRows, errors);

      // 7. Cập nhật kết quả cuối cùng
      int totalExcelDataRows = validRows.size() + errors.size();
      int failedCount = totalExcelDataRows - result.getSuccessCount();
      updateHistorySuccess(history, totalExcelDataRows, result.getSuccessCount(), failedCount, errors);

      ImportResultResponse.ImportStatistics statistics = ImportResultResponse.ImportStatistics.builder()
          .headerRow(adapter.getHeaderRowIndex() + 1)
          .dataStartRow(adapter.getDataStartRow() + 1)
          .totalWeeksDetected(adapter.getDetectedWeeksCount())
          .mappedColumnsCount(adapter.getMappedColumnsCount())
          .subjectsProcessed(result.getSubjectsProcessed())
          .classSectionsProcessed(result.getClassSectionsProcessed())
          .classSchedulesSaved(result.getClassSchedulesSaved())
          .onlineRoomsAllocated(result.getOnlineRoomsAllocated())
          .meetingSessionsGenerated(result.getMeetingSessionsGenerated())
          .build();

      return ImportResultResponse.builder()
          .importId(history.getId())
          .fileName(file.getOriginalFilename())
          .fileSize(formatFileSize(file.getSize()))
          .semesterId(semester.getId())
          .semesterCode(semester.getCode())
          .semesterName(semester.getName())
          .status(failedCount == 0 ? ImportStatus.SUCCESS : ImportStatus.PARTIAL_SUCCESS)
          .totalRows(totalExcelDataRows)
          .successRows(result.getSuccessCount())
          .failureRows(failedCount)
          .statistics(statistics)
          .errors(errors)
          .completedAt(LocalDateTime.now())
          .build();

    } catch (Exception e) {
      log.error("(importMasterTimetable) System error during timetable import: ", e);
      updateHistoryFailed(history, e.getMessage());
      throw e;
    }
  }

  private void checkConcurrentImport(Long semesterId) {
    historyRepository.findFirstBySemesterIdAndStatusOrderByCreatedAtDesc(semesterId, ImportStatus.PROCESSING)
        .ifPresent(h -> {
          // If previous import job has been running for more than 3 minutes (usually due to crash/restart mid-process)
          if (h.getCreatedAt() != null && h.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusMinutes(3))) {
            log.warn("(checkConcurrentImport) Found stale import job id={} running for more than 3 minutes, automatically marking as FAILED", h.getId());
            h.setStatus(ImportStatus.FAILED);
            try {
              h.setErrorLog(objectMapper.writeValueAsString(java.util.Map.of("reason", "Import process timed out or interrupted due to server restart")));
            } catch (Exception ignored) {
              h.setErrorLog("{\"reason\": \"Import process interrupted\"}");
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
      log.error("(parseExcel) Excel structure business error: ", be);
      throw be;
    } catch (Exception e) {
      log.error("(parseExcel) Error reading or parsing Excel file: ", e);
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
      log.warn("(updateHistorySuccess) Error serializing error_log to JSON", e);
      history.setErrorLog(null);
    }
    historyRepository.save(history);
  }

  private void updateHistoryFailed(ImportHistory history, String error) {
    history.setStatus(ImportStatus.FAILED);
    try {
      history.setErrorLog(objectMapper.writeValueAsString(java.util.Map.of("error", error != null ? error : "Unknown error")));
    } catch (Exception e) {
      log.warn("(updateHistoryFailed) Error serializing error_log to JSON", e);
      history.setErrorLog("{\"error\": \"Unknown system error\"}");
    }
    historyRepository.save(history);
  }

  private String formatFileSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    char pre = "KMGTPE".charAt(exp - 1);
    return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
  }
}