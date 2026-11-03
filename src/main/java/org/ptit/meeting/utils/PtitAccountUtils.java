package org.ptit.meeting.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Tiện ích xử lý chuẩn hóa tài khoản, mã sinh viên và email đặc thù của Học viện PTIT.
 */
public final class PtitAccountUtils {

  // Mật khẩu mặc định khi phòng giáo vụ khởi tạo tài khoản hàng loạt
  public static final String DEFAULT_STUDENT_PASSWORD = "Ptit@123";

  // Domain email sinh viên Microsoft 365 chính thức của PTIT
  public static final String STUDENT_EMAIL_DOMAIN = "@stu.ptit.edu.vn";

  // Regex nhận diện mã sinh viên PTIT hệ chính quy dạng rút gọn trong email (VD: B22CN356, B21AT123, B22CQCN01)
  private static final Pattern SHORT_STUDENT_CODE_PATTERN = Pattern.compile(
      "^B\\d{2}[A-Z]{2,4}\\d+$");

  // Regex phát hiện ký tự dấu tiếng Việt sau khi phân tách Unicode
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");

  private PtitAccountUtils() {
    // Utility class không cho phép khởi tạo thể hiện
  }

  /**
   * Loại bỏ dấu tiếng Việt chuẩn Unicode NFKD. Ví dụ: "Huế" -> "Hue", "Đặng" -> "Dang".
   */
  public static String removeAccents(String str) {
    if (str == null || str.isBlank()) {
      return "";
    }
    String nfkd = Normalizer.normalize(str, Normalizer.Form.NFKD);
    return DIACRITICS_PATTERN.matcher(nfkd).replaceAll("")
        .replace("đ", "d")
        .replace("Đ", "D");
  }

  /**
   * Sinh email sinh viên PTIT theo quy ước Office 365:
   * [Tên][ChữCáiĐầuHọĐệm].[MãRútGọn]@stu.ptit.edu.vn Ví dụ: ("Phan Thị Hồng Huế", "B22DCCN356") ->
   * "HuePTH.B22CN356@stu.ptit.edu.vn"
   */
  public static String generateStudentEmail(String fullName, String studentCode) {
    if (studentCode == null || studentCode.isBlank()) {
      return null;
    }
    String cleanCode = normalizeToShortStudentCode(studentCode);

    if (fullName == null || fullName.isBlank()) {
      return cleanCode.toLowerCase() + STUDENT_EMAIL_DOMAIN;
    }

    String[] parts = fullName.trim().split("\\s+");
    String lastName = removeAccents(parts[parts.length - 1]);
    if (!lastName.isEmpty()) {
      lastName = Character.toUpperCase(lastName.charAt(0)) + lastName.substring(1).toLowerCase();
    }

    StringBuilder initials = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      String p = removeAccents(parts[i]);
      if (!p.isEmpty()) {
        initials.append(Character.toUpperCase(p.charAt(0)));
      }
    }

    return lastName + initials + "." + cleanCode + STUDENT_EMAIL_DOMAIN;
  }

  /**
   * Kiểm tra xem một chuỗi có phải là mã sinh viên dạng rút gọn hay không (thiếu "DC", VD: B22CN356).
   */
  public static boolean isShortStudentCode(String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    String clean = code.trim().toUpperCase();
    return SHORT_STUDENT_CODE_PATTERN.matcher(clean).matches() && !clean.contains("DC");
  }

  /**
   * Khôi phục mã sinh viên đầy đủ có "DC" (Đại học Chính quy). Ví dụ: "B22CN356" -> "B22DCCN356".
   */
  public static String normalizeToFullStudentCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    String clean = code.trim().toUpperCase();
    if (isShortStudentCode(clean)) {
      return clean.replaceFirst("^(B\\d{2})([A-Z]{2,4}\\d+)$", "$1DC$2");
    }
    return clean;
  }

  /**
   * Lược bỏ "DC" khỏi mã sinh viên (tương thích định dạng mã email Outlook). Ví dụ: "B22DCCN356" ->
   * "B22CN356".
   */
  public static String normalizeToShortStudentCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    String clean = code.trim().toUpperCase();
    if (clean.contains("DC")) {
      return clean.replaceFirst("^(B\\d{2})DC(.*)$", "$1$2");
    }
    return clean;
  }
}