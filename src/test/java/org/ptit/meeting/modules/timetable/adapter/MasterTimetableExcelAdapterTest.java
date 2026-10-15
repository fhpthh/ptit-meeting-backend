package org.ptit.meeting.modules.timetable.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.ptit.meeting.modules.timetable.adapter.impl.MasterTimetableExcelAdapter;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

class MasterTimetableExcelAdapterTest {

  @Test
  void testScanAndParseRealExcel() throws Exception {
    File file = new File("1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx");
    assertTrue(file.exists(), "File Excel mẫu phải tồn tại trong project");

    MasterTimetableExcelAdapter adapter = new MasterTimetableExcelAdapter();

    try (FileInputStream fis = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(fis)) {

      Sheet sheet = workbook.getSheetAt(0);

      // 1. Kiểm tra khởi tạo header
      assertDoesNotThrow(() -> adapter.initializeHeader(sheet));

      int startRow = adapter.getDataStartRow();
      assertEquals(11, startRow, "Dòng dữ liệu đầu tiên phải là dòng 12 (index 11)");

      // 2. Parse thử dòng đầu tiên
      List<ImportRowError> errors = new ArrayList<>();
      Row firstDataRow = sheet.getRow(startRow);
      assertNotNull(firstDataRow);

      MasterTimetableRowDto dto = adapter.adapt(firstDataRow, startRow + 1, errors);
      assertNotNull(dto, "Dòng đầu tiên phải parse thành công");
      assertEquals("AI1306", dto.getSubjectCode());
      assertEquals("01", dto.getGroupNumber());
      assertEquals(3, dto.getDayOfWeek());
      assertEquals(8, dto.getStartPeriod());
      assertEquals(2, dto.getNumPeriods());
      assertFalse(dto.getActiveWeeks().isEmpty(), "Phải tìm thấy các tuần học active");
      assertTrue(dto.getActiveWeeks().contains(1), "Dòng 12 phải học từ tuần 1");
    }
  }
}
