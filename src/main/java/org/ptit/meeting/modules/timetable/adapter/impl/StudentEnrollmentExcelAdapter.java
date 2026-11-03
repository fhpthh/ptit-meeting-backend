package org.ptit.meeting.modules.timetable.adapter.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.adapter.ExcelImportAdapter;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.internal.StudentEnrollmentRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentEnrollmentExcelAdapter implements ExcelImportAdapter<StudentEnrollmentRowDto> {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();

  // Từ khóa nhận diện cột
  private static final String COL_STT = "stt";
  private static final String COL_MSV = "msv";
  private static final String COL_NAME = "hoten";
  private static final String COL_DOB = "ngaysinh";
  private static final String COL_CLASS = "lop";

  private final ThreadLocal<Map<String, Integer>> columnMapContext = ThreadLocal.withInitial(HashMap::new);
  private final ThreadLocal<Integer> headerRowIndexContext = ThreadLocal.withInitial(() -> 0);

  @Override
  public ImportType getSupportedType() {
    return ImportType.STUDENT_ENROLMENT;
  }

  @Override
  public void initializeHeader(Sheet sheet) {
    Map<String, Integer> columnMap = new HashMap<>();
    int headerIndex = -1;

    // Quét 10 dòng đầu để tìm dòng header chứa cột MSV
    for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;

      for (int c = 0; c < row.getLastCellNum(); c++) {
        String val = getCellStringValue(row.getCell(c)).toLowerCase().replaceAll("[\\s_.]", "");
        if (val.contains("msv") || val.contains("masv") || val.contains("sinhvien")) {
          headerIndex = r;
          break;
        }
      }
      if (headerIndex != -1) break;
    }

    if (headerIndex == -1) {
      log.error("(initializeHeader) Cannot find header row in student list Excel file");
      throw BusinessException.invalidInputFile(TimetableErrorConstants.EXCEL_HEADER_INVALID);
    }

    headerRowIndexContext.set(headerIndex);
    Row headerRow = sheet.getRow(headerIndex);

    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
      String cleanVal = PtitAccountUtils.removeAccents(getCellStringValue(headerRow.getCell(c)))
          .toLowerCase().replaceAll("[\\s_.]", "");

      if (cleanVal.contains("stt") || cleanVal.contains("sott")) {
        columnMap.put(COL_STT, c);
      } else if (cleanVal.contains("msv") || cleanVal.contains("masv") || cleanVal.contains("sinhvien")) {
        columnMap.put(COL_MSV, c);
      } else if (cleanVal.contains("hoten") || cleanVal.contains("ten") || cleanVal.contains("hovaten")) {
        columnMap.put(COL_NAME, c);
      } else if (cleanVal.contains("ngaysinh") || cleanVal.contains("dob") || cleanVal.contains("ns")) {
        columnMap.put(COL_DOB, c);
      } else if (cleanVal.contains("lop") || cleanVal.contains("class")) {
        columnMap.put(COL_CLASS, c);
      }
    }

    if (!columnMap.containsKey(COL_MSV)) {
      throw BusinessException.invalidInputFile(TimetableErrorConstants.EXCEL_HEADER_INVALID);
    }

    columnMapContext.set(columnMap);
    log.info("(initializeHeader) Detected header at row: {}, mapped columns: {}", headerIndex + 1, columnMap);
  }

  @Override
  public int getDataStartRow() {
    return headerRowIndexContext.get() + 1;
  }

  @Override
  public StudentEnrollmentRowDto adapt(Row row, int rowNumber, List<ImportRowError> errors) {
    Map<String, Integer> columnMap = columnMapContext.get();
    String msv = getMappedValue(row, columnMap.get(COL_MSV));
    if (msv == null || msv.isBlank()) {
      return null; // Bỏ qua dòng trống
    }

    String fullName = getMappedValue(row, columnMap.get(COL_NAME));
    String managementClass = getMappedValue(row, columnMap.get(COL_CLASS));
    String dob = getMappedDateValue(row, columnMap.get(COL_DOB));

    return StudentEnrollmentRowDto.builder()
        .studentCode(msv.trim().toUpperCase())
        .fullName(fullName != null ? fullName.trim() : null)
        .dob(dob)
        .managementClass(managementClass != null ? managementClass.trim() : null)
        .build();
  }

  @Override
  public boolean validate(StudentEnrollmentRowDto data, int rowNumber, List<ImportRowError> errors) {
    if (data.getStudentCode() == null || data.getStudentCode().isBlank()) {
      errors.add(new ImportRowError(rowNumber, "Mã sinh viên không được để trống"));
      return false;
    }
    return true;
  }

  private String getMappedValue(Row row, Integer colIdx) {
    if (colIdx == null) return null;
    return getCellStringValue(row.getCell(colIdx));
  }

  private String getMappedDateValue(Row row, Integer colIdx) {
    if (colIdx == null) return null;
    Cell cell = row.getCell(colIdx);
    if (cell == null) return null;

    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().toString();
    }
    return getCellStringValue(cell);
  }

  private String getCellStringValue(Cell cell) {
    if (cell == null) return "";
    return DATA_FORMATTER.formatCellValue(cell).trim();
  }
}