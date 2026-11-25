package org.ptit.meeting.modules.auth.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.dto.response.LecturerImportResponse;
import org.ptit.meeting.modules.auth.service.LecturerImportService;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LecturerImportServiceImpl implements LecturerImportService {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();
  private final UserService userService;

  @Override
  @Transactional
  public LecturerImportResponse importLecturersFromExcel(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(StatusCode.INVALID_INPUT_FILE,
          AuthErrorConstants.LECTURER_IMPORT_EMPTY_FILE);
    }

    List<LecturerImportResponse.RowError> errors = new ArrayList<>();
    int successCount = 0;
    int totalRows = 0;

    try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheetAt(0);
      if (sheet.getLastRowNum() < 1) {
        throw new BusinessException(StatusCode.INVALID_INPUT_FILE,
            AuthErrorConstants.LECTURER_IMPORT_EMPTY_FILE);
      }

      // 1. Quét tìm header linh hoạt
      Map<String, Integer> colMap = detectHeaderColumns(sheet);
      int headerRow = colMap.get("_headerRow");

      // Set lưu email đã cấp trong đợt import này để tránh trùng nhau giữa 2 thầy cùng tên
      Set<String> allocatedEmailsInBatch = new HashSet<>();

      // 2. Đọc từng dòng dữ liệu
      for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        totalRows++;
        int rowNumber = r + 1;

        try {
          String fullName = getCellVal(row, colMap.get("fullName"));
          String code = getCellVal(row, colMap.get("code"));
          String email = getCellVal(row, colMap.get("email"));

          if (fullName.isBlank() || code.isBlank()) {
            errors.add(new LecturerImportResponse.RowError(rowNumber, code,
                "Thiếu họ tên hoặc mã giảng viên"));
            continue;
          }

          // Xử lý email: Ưu tiên email trong file, nếu trống thì tự sinh
          String targetEmail = (email != null && !email.isBlank())
              ? email.trim().toLowerCase()
              : generateUniqueLecturerEmail(fullName, code, allocatedEmailsInBatch);

          allocatedEmailsInBatch.add(targetEmail);

          // Nhận diện đơn vị từ mã giảng viên
          String department = PtitAccountUtils.resolveLecturerDepartment(code);

          // Lưu hoặc cập nhật tài khoản
          userService.findOrCreateLecturer(code, fullName, targetEmail, department);
          successCount++;
        } catch (Exception ex) {
          log.error("(importLecturersFromExcel) Error at row {}: {}", rowNumber, ex.getMessage());
          errors.add(new LecturerImportResponse.RowError(rowNumber, "", ex.getMessage()));
        }
      }
    } catch (BusinessException be) {
      throw be;
    } catch (Exception e) {
      log.error("(importLecturersFromExcel) Failed to parse Excel file", e);
      throw new BusinessException(StatusCode.INVALID_INPUT_FILE,
          AuthErrorConstants.LECTURER_IMPORT_INVALID_FORMAT);
    }

    return LecturerImportResponse.builder()
        .totalRows(totalRows)
        .successCount(successCount)
        .failureCount(errors.size())
        .errors(errors)
        .build();
  }

  private String generateUniqueLecturerEmail(String fullName, String code,
      Set<String> batchEmails) {
    String baseEmail = PtitAccountUtils.generateStaffEmail(fullName);
    if (!batchEmails.contains(baseEmail)) {
      return baseEmail;
    }
    // Xử lý collision nếu trùng tên: gán thêm mã giảng viên
    return baseEmail.replace("@ptit.edu.vn", "." + code.toLowerCase() + "@ptit.edu.vn");
  }

  private Map<String, Integer> detectHeaderColumns(Sheet sheet) {
    Map<String, Integer> map = new HashMap<>();
    for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      for (int c = 0; c < row.getLastCellNum(); c++) {
        String val = PtitAccountUtils.removeAccents(DATA_FORMATTER.formatCellValue(row.getCell(c)))
            .toLowerCase().replaceAll("[\\s_.]", "");
        if (val.contains("magiangvien") || val.contains("magv") || val.contains("macb")) {
          map.put("code", c);
        } else if (val.contains("hoten") || val.contains("tengiangvien")) {
          map.put("fullName", c);
        } else if (val.contains("email") || val.contains("mail")) {
          map.put("email", c);
        } else if (val.contains("stt")) {
          map.put("stt", c);
        }
      }
      if (map.containsKey("code") && map.containsKey("fullName")) {
        map.put("_headerRow", r);
        return map;
      }
    }
    throw new BusinessException(StatusCode.INVALID_INPUT_FILE,
        AuthErrorConstants.LECTURER_IMPORT_INVALID_FORMAT);
  }

  private String getCellVal(Row row, Integer colIdx) {
    if (colIdx == null || colIdx < 0) {
      return "";
    }
    return DATA_FORMATTER.formatCellValue(row.getCell(colIdx)).trim();
  }

  private boolean isRowEmpty(Row row) {
    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
      if (!DATA_FORMATTER.formatCellValue(row.getCell(c)).trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }
}