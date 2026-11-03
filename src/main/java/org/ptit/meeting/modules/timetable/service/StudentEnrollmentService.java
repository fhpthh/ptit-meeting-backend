package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.internal.StudentEnrollmentRowDto;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

/**
 * Service chuyên trách nghiệp vụ ghi danh sinh viên và cấp tài khoản lớp học phần.
 */
public interface StudentEnrollmentService {

  TimetableProcessResult processStudentEnrollments(
      Long classSectionId,
      List<StudentEnrollmentRowDto> rows,
      List<ImportRowError> errors
  );
}