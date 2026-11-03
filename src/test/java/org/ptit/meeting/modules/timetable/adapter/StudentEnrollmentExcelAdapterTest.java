package org.ptit.meeting.modules.timetable.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.ptit.meeting.modules.timetable.adapter.impl.StudentEnrollmentExcelAdapter;
import org.ptit.meeting.modules.timetable.dto.internal.StudentEnrollmentRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

class StudentEnrollmentExcelAdapterTest {

  private StudentEnrollmentExcelAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new StudentEnrollmentExcelAdapter();
  }

  @Test
  void testParseDSSVFile() throws Exception {
    File file = new File("DSSV.xlsx");
    assertTrue(file.exists(), "File DSSV.xlsx phải tồn tại trong thư mục gốc");

    try (FileInputStream fis = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(fis)) {

      Sheet sheet = workbook.getSheetAt(0);

      // 1. Khởi tạo và quét Header
      assertDoesNotThrow(() -> adapter.initializeHeader(sheet));
      int startRow = adapter.getDataStartRow();
      assertEquals(1, startRow, "Dữ liệu sinh viên bắt đầu từ dòng 2 (index 1)");

      // 2. Parse các dòng sinh viên
      List<ImportRowError> errors = new ArrayList<>();
      List<StudentEnrollmentRowDto> validRows = new ArrayList<>();

      for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        int rowNumber = i + 1;
        StudentEnrollmentRowDto dto = adapter.adapt(row, rowNumber, errors);
        if (dto != null && adapter.validate(dto, rowNumber, errors)) {
          validRows.add(dto);
        }
      }

      // Kiểm tra có đúng 60 sinh viên được đọc thành công
      assertEquals(60, validRows.size(), "Phải đọc thành công 60 sinh viên trong file DSSV.xlsx");
      assertTrue(errors.isEmpty(), "Không có lỗi dòng nào trong file chuẩn");

      // Kiểm tra dòng đầu tiên: Lê Việt Anh, B22DCCN020
      StudentEnrollmentRowDto first = validRows.get(0);
      assertEquals("B22DCCN020", first.getStudentCode());
      assertEquals("Lê Việt Anh", first.getFullName());
      assertEquals("D22CNPM04", first.getManagementClass());

      // Kiểm tra dòng 32 (index 31): Phan Thị Hồng Huế, B22DCCN356
      StudentEnrollmentRowDto hueStudent = validRows.stream()
          .filter(s -> "B22DCCN356".equals(s.getStudentCode()))
          .findFirst()
          .orElse(null);

      assertNotNull(hueStudent, "Phải tìm thấy sinh viên Phan Thị Hồng Huế (B22DCCN356)");
      assertEquals("Phan Thị Hồng Huế", hueStudent.getFullName());
      assertEquals("D22CNPM04", hueStudent.getManagementClass());
    }
  }
}
