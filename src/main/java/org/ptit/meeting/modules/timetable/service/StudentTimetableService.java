package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;

public interface StudentTimetableService {

  List<StudentSemesterResponse> getAvailableSemesters(Long studentId);

  StudentWeeklyTimetableResponse getWeeklyTimetable(
      Long studentId,
      Long semesterId,
      int weekNumber
  );
}