package org.ptit.meeting.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PtitAccountUtilsTest {

  @Test
  void testGenerateStudentEmail() {
    // Trường hợp thực tế của user: Phan Thị Hồng Huế, MSV B22DCCN356
    String email1 = PtitAccountUtils.generateStudentEmail("Phan Thị Hồng Huế", "B22DCCN356");
    assertEquals("HuePTH.B22CN356@stu.ptit.edu.vn", email1);

    // Trường hợp Lê Việt Anh, MSV B22DCCN020
    String email2 = PtitAccountUtils.generateStudentEmail("Lê Việt Anh", "B22DCCN020");
    assertEquals("AnhLV.B22CN020@stu.ptit.edu.vn", email2);

    // Trường hợp Trần Đào Gia Bảo, MSV B22DCCN069
    String email3 = PtitAccountUtils.generateStudentEmail("Trần Đào Gia Bảo", "B22DCCN069");
    assertEquals("BaoTDG.B22CN069@stu.ptit.edu.vn", email3);

    // Khi truyền mã đã rút gọn sẵn (B22CN356)
    String email4 = PtitAccountUtils.generateStudentEmail("Phan Thị Hồng Huế", "B22CN356");
    assertEquals("HuePTH.B22CN356@stu.ptit.edu.vn", email4);

    // Khi không có họ tên
    String email5 = PtitAccountUtils.generateStudentEmail("", "B22DCCN356");
    assertEquals("b22cn356@stu.ptit.edu.vn", email5);

    // Khi mã null
    assertNull(PtitAccountUtils.generateStudentEmail("Nguyen Van A", null));
  }

  @Test
  void testNormalizeToFullStudentCode() {
    // Khôi phục mã rút gọn từ Outlook về mã chuẩn có "DC"
    assertEquals("B22DCCN356", PtitAccountUtils.normalizeToFullStudentCode("B22CN356"));
    assertEquals("B22DCCN020", PtitAccountUtils.normalizeToFullStudentCode("b22cn020"));
    assertEquals("B21DCAT123", PtitAccountUtils.normalizeToFullStudentCode("B21AT123"));

    // Nếu mã đã có sẵn "DC", giữ nguyên
    assertEquals("B22DCCN356", PtitAccountUtils.normalizeToFullStudentCode("B22DCCN356"));
    assertEquals("B22DCCN356", PtitAccountUtils.normalizeToFullStudentCode("b22dccn356"));

    assertNull(PtitAccountUtils.normalizeToFullStudentCode(null));
  }

  @Test
  void testNormalizeToShortStudentCode() {
    // Lược bỏ "DC"
    assertEquals("B22CN356", PtitAccountUtils.normalizeToShortStudentCode("B22DCCN356"));
    assertEquals("B22CN020", PtitAccountUtils.normalizeToShortStudentCode("b22dccn020"));

    // Nếu đã không có "DC", giữ nguyên
    assertEquals("B22CN356", PtitAccountUtils.normalizeToShortStudentCode("B22CN356"));
  }

  @Test
  void testRemoveAccents() {
    assertEquals("Hue", PtitAccountUtils.removeAccents("Huế"));
    assertEquals("Dang", PtitAccountUtils.removeAccents("Đặng"));
    assertEquals("Vu Dinh Duc", PtitAccountUtils.removeAccents("Vũ Đình Đức"));
  }

  @Test
  void testIsShortStudentCode() {
    // Mã rút gọn không có "DC" -> true
    org.junit.jupiter.api.Assertions.assertTrue(PtitAccountUtils.isShortStudentCode("B22CN356"));
    org.junit.jupiter.api.Assertions.assertTrue(PtitAccountUtils.isShortStudentCode("b22cn356"));
    org.junit.jupiter.api.Assertions.assertTrue(PtitAccountUtils.isShortStudentCode("B21AT123"));

    // Mã đầy đủ có "DC" -> false
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("B22DCCN356"));
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("b22dccn356"));
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("B21DCAT123"));

    // Email trường -> false
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("HuePTH.B22CN356@stu.ptit.edu.vn"));

    // Username nhân viên/giảng viên/admin -> false
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("admin"));
    org.junit.jupiter.api.Assertions.assertFalse(PtitAccountUtils.isShortStudentCode("gv_nguyen"));
  }
}
