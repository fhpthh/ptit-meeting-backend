package org.ptit.meeting.modules.timetable.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ptit.meeting.modules.timetable.adapter.impl.MasterTimetableExcelAdapter;
import org.ptit.meeting.modules.timetable.config.TimetableProperties;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.service.PeriodTimeCalculator;

class MasterTimetableExcelAdapterTest {

  private MasterTimetableExcelAdapter adapter;

  @BeforeEach
  void setUp() {
    PeriodTimeCalculator calculator = new PeriodTimeCalculator(new TimetableProperties());
    calculator.init();
    adapter = new MasterTimetableExcelAdapter(calculator);
  }

  @Test
  void testScanAndParseRealExcel() throws Exception {
    File file = new File("uploads/timetables/1792090163847_c028c092.xlsx");
    if (!file.exists()) {
      file = new File("1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx");
    }
    assertTrue(file.exists(), "File Excel mẫu phải tồn tại trong project");

    try (FileInputStream fis = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(fis)) {

      Sheet sheet = workbook.getSheetAt(0);

      // 1. Khởi tạo header
      assertDoesNotThrow(() -> adapter.initializeHeader(sheet));
      int startRow = adapter.getDataStartRow();

      // 2. Parse thử dòng đầu tiên
      List<ImportRowError> errors = new ArrayList<>();
      Row firstDataRow = sheet.getRow(startRow);
      assertNotNull(firstDataRow);

      MasterTimetableRowDto dto = adapter.adapt(firstDataRow, startRow + 1, errors);
      assertNotNull(dto, "Dòng đầu tiên phải parse thành công");

      // Kiểm tra tính giờ tròn chính xác
      assertNotNull(dto.getStartTime());
      assertNotNull(dto.getEndTime());
      assertTrue(dto.getEndTime().isAfter(dto.getStartTime()));
      assertFalse(dto.getActiveWeeks().isEmpty(), "Phải tìm thấy các tuần học active");
    }
  }
}