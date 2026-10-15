package org.ptit.meeting.modules.timetable.adapter.impl;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.adapter.ExcelImportAdapter;
import org.ptit.meeting.modules.timetable.constant.TimetableConstants;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MasterTimetableExcelAdapter implements ExcelImportAdapter<MasterTimetableRowDto> {

  private final Map<String, Integer> colIndexMap = new HashMap<>();
  private final Map<Integer, Integer> weekToColMap = new HashMap<>(); // Tuần (1..25) -> Cột index
  private int dataStartRow = 12;

  @Override
  public ImportType getSupportedType() {
    return ImportType.MASTER_TIMETABLE;
  }

  @Override
  public void initializeHeader(Sheet sheet) {
    colIndexMap.clear();
    weekToColMap.clear();

    int headerRowIndex = scanHeaderRow(sheet);
    mapCoreColumns(sheet.getRow(headerRowIndex));
    mapWeekColumnsByNumber(sheet, headerRowIndex);
    determineDataStartRow(sheet, headerRowIndex);
  }

  private int scanHeaderRow(Sheet sheet) {
    int maxScan = Math.min(25, sheet.getLastRowNum());
    for (int r = 0; r <= maxScan; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      boolean hasSubject = false, hasDay = false, hasPeriod = false;
      for (Cell cell : row) {
        String text = normalize(getCellValueAsString(cell));
        if (text.contains("mamon")) {
          hasSubject = true;
        }
        if (text.equals("thu")) {
          hasDay = true;
        }
        if (text.contains("tietbd")) {
          hasPeriod = true;
        }
      }
      if (hasSubject && hasDay && hasPeriod) {
        return r;
      }
    }
    throw BusinessException.invalidInputFile(TimetableErrorConstants.EXCEL_HEADER_INVALID);
  }

  private void mapCoreColumns(Row headerRow) {
    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
      String text = normalize(getCellValueAsString(headerRow.getCell(c)));
      if (text.contains("mamon")) {
        colIndexMap.put(TimetableConstants.HEADER_SUBJECT_CODE, c);
      } else if (text.contains("tenmon")) {
        colIndexMap.put(TimetableConstants.HEADER_SUBJECT_NAME, c);
      } else if (text.equals("khoa")) {
        colIndexMap.put(TimetableConstants.HEADER_COHORT, c);
      } else if (text.equals("nhom")) {
        colIndexMap.put(TimetableConstants.HEADER_GROUP, c);
      } else if (text.contains("toth")) {
        colIndexMap.put(TimetableConstants.HEADER_PRACTICE, c);
      } else if (text.equals("thu")) {
        colIndexMap.put(TimetableConstants.HEADER_DAY, c);
      } else if (text.contains("tietbd")) {
        colIndexMap.put(TimetableConstants.HEADER_START_PERIOD, c);
      } else if (text.contains("sotiet")) {
        colIndexMap.put(TimetableConstants.HEADER_NUM_PERIODS, c);
      } else if (text.contains("giangvien")) {
        colIndexMap.put(TimetableConstants.HEADER_LECTURER, c);
      } else if (text.equals("phong")) {
        colIndexMap.put(TimetableConstants.HEADER_ROOM, c);
      } else if (text.equals("nha")) {
        colIndexMap.put(TimetableConstants.HEADER_BUILDING, c);
      } else if (text.contains("malop")) {
        colIndexMap.put(TimetableConstants.HEADER_MANAGEMENT_CLASS, c);
      } else if (text.contains("sotc")) {
        colIndexMap.put(TimetableConstants.HEADER_CREDIT, c);
      }
    }
  }

  private void mapWeekColumnsByNumber(Sheet sheet, int headerRowIndex) {
    // Quét các dòng quanh header (như dòng 5 hoặc 10) để nhặt các cột có số tuần từ 1 đến 25
    int startScan = Math.max(0, headerRowIndex - 3);
    for (int r = startScan; r <= headerRowIndex + 3; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      for (int c = 0; c < row.getLastCellNum(); c++) {
        Integer val = getCellValueAsInteger(row.getCell(c));
        if (val != null && val >= 1 && val <= 25) {
          weekToColMap.putIfAbsent(val, c);
        }
      }
    }
  }

  private void determineDataStartRow(Sheet sheet, int headerRowIndex) {
    this.dataStartRow = headerRowIndex + 1;
    int dayCol = colIndexMap.getOrDefault(TimetableConstants.HEADER_DAY, 9);
    while (this.dataStartRow <= sheet.getLastRowNum()) {
      Row r = sheet.getRow(this.dataStartRow);
      if (r != null) {
        Integer dayVal = getCellValueAsInteger(r.getCell(dayCol));
        if (dayVal != null && dayVal >= 2 && dayVal <= 8) {
          break;
        }
      }
      this.dataStartRow++;
    }
  }

  @Override
  public int getDataStartRow() {
    return this.dataStartRow;
  }

  @Override
  public MasterTimetableRowDto adapt(Row row, int rowNumber, List<ImportRowError> errors) {
    try {
      int dayCol = colIndexMap.getOrDefault(TimetableConstants.HEADER_DAY, -1);
      Integer dayOfWeek = dayCol != -1 ? getCellValueAsInteger(row.getCell(dayCol)) : null;
      if (dayOfWeek == null || dayOfWeek < 2 || dayOfWeek > 8) {
        return null;
      }

      String subjectCode = getVal(row, TimetableConstants.HEADER_SUBJECT_CODE);
      String groupNumber = getVal(row, TimetableConstants.HEADER_GROUP);
      String practiceGroup = getVal(row, TimetableConstants.HEADER_PRACTICE);
      Integer startPeriod = getInt(row, TimetableConstants.HEADER_START_PERIOD);
      Integer numPeriods = getInt(row, TimetableConstants.HEADER_NUM_PERIODS);
      String roomText = getVal(row, TimetableConstants.HEADER_ROOM);

      Set<Integer> activeWeeks = extractActiveWeeks(row);
      boolean isOnline = TimetableConstants.KEYWORD_ONLINE_EN.equalsIgnoreCase(
          roomText != null ? roomText.trim() : "");

      return MasterTimetableRowDto.builder()
          .rowNumber(rowNumber)
          .subjectCode(subjectCode)
          .subjectName(getVal(row, TimetableConstants.HEADER_SUBJECT_NAME))
          .cohort(getVal(row, TimetableConstants.HEADER_COHORT))
          .classCode(generateClassCode(subjectCode, groupNumber, practiceGroup))
          .groupNumber(groupNumber)
          .practiceGroup(practiceGroup)
          .managementClass(getVal(row, TimetableConstants.HEADER_MANAGEMENT_CLASS))
          .lecturerName(getVal(row, TimetableConstants.HEADER_LECTURER))
          .dayOfWeek(dayOfWeek)
          .startPeriod(startPeriod)
          .numPeriods(numPeriods)
          .startTime(calculateStartTime(startPeriod))
          .endTime(calculateEndTime(startPeriod, numPeriods))
          .roomText(roomText)
          .building(getVal(row, TimetableConstants.HEADER_BUILDING))
          .credit(getInt(row, TimetableConstants.HEADER_CREDIT))
          .activeWeeks(activeWeeks)
          .isOnline(isOnline)
          .build();
    } catch (Exception e) {
      errors.add(new ImportRowError(rowNumber, "Lỗi đọc dữ liệu: " + e.getMessage()));
      return null;
    }
  }

  private Set<Integer> extractActiveWeeks(Row row) {
    Set<Integer> activeWeeks = new TreeSet<>();
    for (Map.Entry<Integer, Integer> entry : weekToColMap.entrySet()) {
      int weekNum = entry.getKey();
      int colIdx = entry.getValue();
      String val = getCellValueAsString(row.getCell(colIdx));
      if (val != null && val.trim().equalsIgnoreCase("x")) {
        activeWeeks.add(weekNum);
      }
    }
    return activeWeeks;
  }

  @Override
  public boolean validate(MasterTimetableRowDto data, int rowNumber, List<ImportRowError> errors) {
    if (data == null) {
      return false;
    }
    if (data.getSubjectCode() == null || data.getSubjectCode().isBlank()) {
      errors.add(new ImportRowError(rowNumber, "Mã môn học không được để trống"));
      return false;
    }
    if (data.getActiveWeeks() == null || data.getActiveWeeks().isEmpty()) {
      errors.add(new ImportRowError(rowNumber, "Không có tuần học nào được đánh dấu 'x'"));
      return false;
    }
    return true;
  }

  private String getVal(Row row, String key) {
    int col = colIndexMap.getOrDefault(key, -1);
    return col != -1 ? getCellValueAsString(row.getCell(col)) : null;
  }

  private Integer getInt(Row row, String key) {
    int col = colIndexMap.getOrDefault(key, -1);
    return col != -1 ? getCellValueAsInteger(row.getCell(col)) : null;
  }

  private String generateClassCode(String subjectCode, String group, String practiceGroup) {
    StringBuilder sb = new StringBuilder(subjectCode != null ? subjectCode.trim() : "");
    if (group != null && !group.isBlank()) {
      sb.append("_").append(group.trim());
    }
    if (practiceGroup != null && !practiceGroup.isBlank()) {
      sb.append("_TH").append(practiceGroup.trim());
    }
    return sb.toString();
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]", "");
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }
    cell.setCellType(CellType.STRING);
    String val = cell.getStringCellValue();
    return val != null ? val.trim() : null;
  }

  private Integer getCellValueAsInteger(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      return (int) cell.getNumericCellValue();
    }
    if (cell.getCellType() == CellType.STRING) {
      try {
        return Integer.parseInt(cell.getStringCellValue().trim());
      } catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  private LocalTime calculateStartTime(Integer startPeriod) {
    if (startPeriod == null) {
      return LocalTime.of(7, 0);
    }
    return switch (startPeriod) {
      case 1 -> LocalTime.of(7, 0);
      case 2 -> LocalTime.of(7, 50);
      case 3 -> LocalTime.of(8, 50);
      case 4 -> LocalTime.of(9, 40);
      case 5 -> LocalTime.of(10, 30);
      case 6 -> LocalTime.of(11, 20);
      case 7 -> LocalTime.of(12, 30);
      case 8 -> LocalTime.of(13, 20);
      case 9 -> LocalTime.of(14, 20);
      case 10 -> LocalTime.of(15, 10);
      case 11 -> LocalTime.of(16, 0);
      case 12 -> LocalTime.of(16, 50);
      default -> LocalTime.of(7, 0);
    };
  }

  private LocalTime calculateEndTime(Integer startPeriod, Integer numPeriods) {
    if (startPeriod == null || numPeriods == null) {
      return LocalTime.of(9, 25);
    }
    return calculateStartTime(startPeriod).plusMinutes((long) numPeriods * 50);
  }
}