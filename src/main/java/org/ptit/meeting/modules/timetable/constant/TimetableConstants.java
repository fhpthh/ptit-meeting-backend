package org.ptit.meeting.modules.timetable.constant;

public final class TimetableConstants {
  private TimetableConstants() {}

  public static final String MODULE_NAME = "timetable";
  public static final String KEYWORD_ONLINE = "ONLINE";
  public static final String KEYWORD_ONLINE_EN = "ONLINE";

  // Buffer thời gian mở phòng
  public static final int LECTURER_JOIN_EARLY_MINUTES = 30;
  public static final int STUDENT_JOIN_EARLY_MINUTES = 15;

  // Từ khóa chuẩn hóa để dò tìm dòng tiêu đề trong Excel
  public static final String HEADER_SUBJECT_CODE = "mamon";
  public static final String HEADER_SUBJECT_NAME = "tenmon";
  public static final String HEADER_COHORT = "khoa";
  public static final String HEADER_GROUP = "nhom";
  public static final String HEADER_PRACTICE = "toth";
  public static final String HEADER_DAY = "thu";
  public static final String HEADER_START_PERIOD = "tietbd";
  public static final String HEADER_NUM_PERIODS = "sotiet";
  public static final String HEADER_LECTURER = "giangvien";
  public static final String HEADER_ROOM = "phong";
  public static final String HEADER_BUILDING = "nha";
  public static final String HEADER_MANAGEMENT_CLASS = "malop";
  public static final String HEADER_CREDIT = "sotc";
}