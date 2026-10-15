package org.ptit.meeting.modules.timetable.constant;

public class TimetableErrorConstants {

  // Lỗi liên quan đến Học kỳ (Semester)
  public static final String SEMESTER_NOT_FOUND = "error.timetable.semester.not-found";
  public static final String SEMESTER_CODE_EXISTED = "error.timetable.semester.code-existed";
  public static final String SEMESTER_START_DATE_INVALID = "error.timetable.semester.start-date-invalid";

  // Lỗi liên quan đến Tệp tin tải lên (File Upload & Format)
  public static final String FILE_EMPTY = "error.timetable.file.empty";
  public static final String FILE_INVALID_FORMAT = "error.timetable.file.invalid-format";
  public static final String FILE_EXCEEDS_SIZE_LIMIT = "error.timetable.file.too-large";
  public static final String EXCEL_HEADER_INVALID = "error.timetable.excel.header-invalid";

  // Lỗi kiểm tra xung đột / trùng lịch (Conflict Detection)
  public static final String LECTURER_SCHEDULE_CONFLICT = "error.timetable.conflict.lecturer";
  public static final String STUDENT_SCHEDULE_CONFLICT = "error.timetable.conflict.student";

  // Lỗi liên quan đến Lớp học phần & Môn học
  public static final String SUBJECT_NOT_FOUND = "error.timetable.subject.not-found";
  public static final String CLASS_SECTION_NOT_FOUND = "error.timetable.class-section.not-found";
  public static final String CLASS_SCHEDULE_NOT_FOUND = "error.timetable.schedule.not-found";

  // Lỗi liên quan đến Buổi học & Quyền vào phòng (Access Control)
  public static final String SESSION_NOT_FOUND = "error.timetable.session.not-found";
  public static final String SESSION_NOT_ACTIVE = "error.timetable.session.not-active";
  public static final String NOT_TIME_TO_JOIN = "error.timetable.session.not-time-to-join";
  public static final String NOT_ENROLLED_IN_CLASS = "error.timetable.access.not-enrolled";

  private TimetableErrorConstants() {
  }

}
