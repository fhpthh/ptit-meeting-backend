package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentOnlineSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentSubjectOnlineScheduleResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;

public interface StudentTimetableService {

  List<StudentSemesterResponse> getAvailableSemesters(Long studentId);

  StudentWeeklyTimetableResponse getWeeklyTimetable(
      Long studentId,
      Long semesterId,
      int weekNumber
  );

  List<StudentOnlineSubjectResponse> getOnlineSubjects(Long studentId, Long semesterId);

  StudentSubjectOnlineScheduleResponse getSubjectOnlineSchedule(
      Long studentId,
      Long semesterId,
      Long subjectId
  );
}
