package org.ptit.meeting.modules.timetable.adapter.impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.adapter.ExcelImportAdapter;
import org.ptit.meeting.modules.timetable.constant.TimetableConstants;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.service.PeriodTimeCalculator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterTimetableExcelAdapter implements ExcelImportAdapter<MasterTimetableRowDto> {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();
  private final PeriodTimeCalculator periodTimeCalculator;
  private final ThreadLocal<SheetParseContext> currentContext = ThreadLocal.withInitial(
      SheetParseContext::new);

  @Override
  public ImportType getSupportedType() {
    return ImportType.MASTER_TIMETABLE;
  }

  @Override
  public void initializeHeader(Sheet sheet) {
    SheetParseContext ctx = new SheetParseContext();
    currentContext.set(ctx);

    ctx.headerRowIndex = scanHeaderRow(sheet);
    mapCoreColumns(sheet.getRow(ctx.headerRowIndex), ctx);
    mapWeekColumnsByNumber(sheet, ctx.headerRowIndex, ctx);
    determineDataStartRow(sheet, ctx.headerRowIndex, ctx);
    extractDetectedStartDate(sheet, ctx);
  }

  public LocalDate getDetectedStartDate() {
    return currentContext.get().detectedStartDate;
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
        if (text.contains("mamon") || text.contains("monhoc") || text.contains("hocphan")) {
          hasSubject = true;
        }
        if (text.equals("thu")) {
          hasDay = true;
        }
        if (text.contains("tietbd") || text.contains("tiet")) {
          hasPeriod = true;
        }
      }
      if (hasSubject && hasDay && hasPeriod) {
        log.info("(scanHeaderRow) Found timetable header row at line {} (index={})", r + 1, r);
        return r;
      }
    }
    throw BusinessException.invalidInputFile(TimetableErrorConstants.EXCEL_HEADER_INVALID);
  }

  private void mapCoreColumns(Row headerRow, SheetParseContext ctx) {
    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
      String text = normalize(getCellValueAsString(headerRow.getCell(c)));
      if (text.contains("mamon")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_SUBJECT_CODE, c);
      } else if (text.contains("tenmon")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_SUBJECT_NAME, c);
      } else if (text.equals("khoa")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_COHORT, c);
      } else if (text.equals("nhom")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_GROUP, c);
      } else if (text.contains("toth")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_PRACTICE, c);
      } else if (text.equals("thu")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_DAY, c);
      } else if (text.contains("tietbd")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_START_PERIOD, c);
      } else if (text.contains("sotiet")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_NUM_PERIODS, c);
      } else if (text.contains("giangvien")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_LECTURER, c);
      } else if (text.equals("phong")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_ROOM, c);
      } else if (text.equals("nha")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_BUILDING, c);
      } else if (text.contains("malop")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_MANAGEMENT_CLASS, c);
      } else if (text.contains("sotc")) {
        ctx.colIndexMap.putIfAbsent(TimetableConstants.HEADER_CREDIT, c);
      }
    }
  }

  private void mapWeekColumnsByNumber(Sheet sheet, int headerRowIndex, SheetParseContext ctx) {
    int startScan = Math.max(0, headerRowIndex - 4);
    int endScan = Math.min(sheet.getLastRowNum(), headerRowIndex + 4);

    for (int r = startScan; r <= endScan; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }

      for (int c = 0; c < row.getLastCellNum() - 2; c++) {
        Integer val1 = getCellValueAsInteger(row.getCell(c));
        Integer val2 = getCellValueAsInteger(row.getCell(c + 1));
        Integer val3 = getCellValueAsInteger(row.getCell(c + 2));

        if (val1 != null && val1 == 1 && val2 != null && val2 == 2 && val3 != null && val3 == 3) {
          int week = 1;
          int col = c;
          while (col < row.getLastCellNum()) {
            Integer weekVal = getCellValueAsInteger(row.getCell(col));
            if (weekVal != null && weekVal == week) {
              ctx.weekToColMap.put(week, col);
              week++;
              col++;
            } else {
              break;
            }
          }
          return;
        }
      }
    }
  }

  private void determineDataStartRow(Sheet sheet, int headerRowIndex, SheetParseContext ctx) {
    ctx.dataStartRow = headerRowIndex + 1;
    int dayCol = ctx.colIndexMap.getOrDefault(TimetableConstants.HEADER_DAY, 9);
    while (ctx.dataStartRow <= sheet.getLastRowNum()) {
      Row r = sheet.getRow(ctx.dataStartRow);
      if (r != null) {
        Integer dayVal = getCellValueAsInteger(r.getCell(dayCol));
        if (dayVal != null && dayVal >= 2 && dayVal <= 8) {
          break;
        }
      }
      ctx.dataStartRow++;
    }
  }

  private void extractDetectedStartDate(Sheet sheet, SheetParseContext ctx) {
    Integer week1Col = ctx.weekToColMap.get(1);
    if (week1Col == null) {
      return;
    }

    try {
      Row monthRow = sheet.getRow(6); // Dòng 7 index 6: Tháng (08/26)
      Row dayRow = sheet.getRow(7);   // Dòng 8 index 7: Ngày BĐ (10)
      if (monthRow == null || dayRow == null) {
        return;
      }

      String monthStr = getCellValueAsString(monthRow.getCell(week1Col));
      Integer dayVal = getCellValueAsInteger(dayRow.getCell(week1Col));

      if (monthStr != null && monthStr.contains("/") && dayVal != null) {
        String[] parts = monthStr.trim().split("/");
        int month = Integer.parseInt(parts[0]);
        int year = 2000 + Integer.parseInt(parts[1]);
        ctx.detectedStartDate = LocalDate.of(year, month, dayVal);
        log.info("(extractDetectedStartDate) Detected Week 1 start date from Excel: {}",
            ctx.detectedStartDate);
      }
    } catch (Exception e) {
      log.warn("(extractDetectedStartDate) Could not parse Week 1 date from Excel header: {}",
          e.getMessage());
    }
  }

  @Override
  public int getDataStartRow() {
    return currentContext.get().dataStartRow;
  }

  @Override
  public int getHeaderRowIndex() {
    return currentContext.get().headerRowIndex;
  }

  @Override
  public int getDetectedWeeksCount() {
    return currentContext.get().weekToColMap.size();
  }

  @Override
  public int getMappedColumnsCount() {
    return currentContext.get().colIndexMap.size();
  }

  @Override
  public MasterTimetableRowDto adapt(Row row, int rowNumber, List<ImportRowError> errors) {
    SheetParseContext ctx = currentContext.get();
    try {
      int dayCol = ctx.colIndexMap.getOrDefault(TimetableConstants.HEADER_DAY, -1);
      Integer dayOfWeek = dayCol != -1 ? getCellValueAsInteger(row.getCell(dayCol)) : null;
      if (dayOfWeek == null || dayOfWeek < 2 || dayOfWeek > 8) {
        return null;
      }

      String subjectCode = getVal(row, TimetableConstants.HEADER_SUBJECT_CODE, ctx);
      String groupNumber = getVal(row, TimetableConstants.HEADER_GROUP, ctx);
      String practiceGroup = getVal(row, TimetableConstants.HEADER_PRACTICE, ctx);
      Integer startPeriod = getInt(row, TimetableConstants.HEADER_START_PERIOD, ctx);
      Integer numPeriods = getInt(row, TimetableConstants.HEADER_NUM_PERIODS, ctx);
      String roomText = getVal(row, TimetableConstants.HEADER_ROOM, ctx);

      Set<Integer> activeWeeks = extractActiveWeeks(row, ctx);
      boolean isOnline = TimetableConstants.KEYWORD_ONLINE_EN.equalsIgnoreCase(
          roomText != null ? roomText.trim() : "");

      // Tính giờ tròn qua PeriodTimeCalculator
      LocalTime startTime = periodTimeCalculator.calculateStartTime(startPeriod);
      LocalTime endTime = periodTimeCalculator.calculateEndTime(startPeriod, numPeriods);

      return MasterTimetableRowDto.builder()
          .rowNumber(rowNumber)
          .subjectCode(subjectCode)
          .subjectName(getVal(row, TimetableConstants.HEADER_SUBJECT_NAME, ctx))
          .cohort(getVal(row, TimetableConstants.HEADER_COHORT, ctx))
          .classCode(generateClassCode(subjectCode, groupNumber, practiceGroup))
          .groupNumber(groupNumber)
          .practiceGroup(practiceGroup)
          .managementClass(getVal(row, TimetableConstants.HEADER_MANAGEMENT_CLASS, ctx))
          .lecturerName(getVal(row, TimetableConstants.HEADER_LECTURER, ctx))
          .dayOfWeek(dayOfWeek)
          .startPeriod(startPeriod)
          .numPeriods(numPeriods)
          .startTime(startTime)
          .endTime(endTime)
          .roomText(roomText)
          .building(getVal(row, TimetableConstants.HEADER_BUILDING, ctx))
          .credit(getInt(row, TimetableConstants.HEADER_CREDIT, ctx))
          .activeWeeks(activeWeeks)
          .isOnline(isOnline)
          .build();
    } catch (Exception e) {
      errors.add(new ImportRowError(rowNumber, "Error reading row data: " + e.getMessage()));
      return null;
    }
  }

  private Set<Integer> extractActiveWeeks(Row row, SheetParseContext ctx) {
    Set<Integer> activeWeeks = new TreeSet<>();
    for (Map.Entry<Integer, Integer> entry : ctx.weekToColMap.entrySet()) {
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
    try {
      periodTimeCalculator.validatePeriodRange(data.getStartPeriod(), data.getNumPeriods());
    } catch (BusinessException be) {
      errors.add(new ImportRowError(rowNumber, be.getMessage()));
      return false;
    }
    return true;
  }

  private String getVal(Row row, String key, SheetParseContext ctx) {
    int col = ctx.colIndexMap.getOrDefault(key, -1);
    return col != -1 ? getCellValueAsString(row.getCell(col)) : null;
  }

  private Integer getInt(Row row, String key, SheetParseContext ctx) {
    int col = ctx.colIndexMap.getOrDefault(key, -1);
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
    String s = text.replace("đ", "d").replace("Đ", "d");
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]", "");
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }
    String val = DATA_FORMATTER.formatCellValue(cell);
    return (val != null && !val.isBlank()) ? val.trim() : null;
  }

  private Integer getCellValueAsInteger(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      return (int) cell.getNumericCellValue();
    }
    String str = DATA_FORMATTER.formatCellValue(cell);
    if (str != null && !str.isBlank()) {
      try {
        return Integer.parseInt(str.trim());
      } catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  // Context lưu cấu hình cột theo từng sheet để đảm bảo Thread-safe cho Spring Singleton
  public static class SheetParseContext {

    public final Map<String, Integer> colIndexMap = new HashMap<>();
    public final Map<Integer, Integer> weekToColMap = new HashMap<>();
    public int dataStartRow = 12;
    public int headerRowIndex = 6;
    public LocalDate detectedStartDate = null;
  }
}