package org.ptit.meeting.modules.timetable.constant;

public class TimetableConstants {

  // --- CẤU HÌNH FILE EXCEL ---
  public static final String EXCEL_XLSX = ".xlsx";
  public static final String EXCEL_XLS = ".xls";
  public static final String EXCEL_CSV = ".csv";
  public static final long MAX_FILE_EXCEL_SIZE = 10 * 1024 * 1024;

  // --- QUY ƯỚC KHUNG GIỜ VÀO PHÒNG (BUFFER TIME) ---
  public static final int LECTURER_JOIN_EARLY_MINUTES = 30;
  public static final int STUDENT_JOIN_EARLY_MINUTES = 15;
  public static final int SESSION_END_GRACE_PERIOD_MINUTES = 10;

  // --- QUY ƯỚC TIẾT HỌC PTIT ---
  public static final int MIN_PERIOD = 1;
  public static final int MAX_PERIOD = 12;

  // --- CÁC TỪ KHÓA NHẬN DIỆN PHÒNG ONLINE TỪ FILE EXCEL ---
  public static final String KEYWORD_ONLINE_VN = "TRỰC TUYẾN";
  public static final String KEYWORD_ONLINE_EN = "ONLINE";
  public static final String KEYWORD_TEAMS = "TEAMS";
  public static final String KEYWORD_ZOOM = "ZOOM";

  // --- MẬT KHẨU MẶC ĐỊNH CHO TÀI KHOẢN SINH VIÊN TẠO MỚI ---
  public static final String DEFAULT_STUDENT_PASSWORD_RAW = "Ptit@123";

  private TimetableConstants() {
  }
}
