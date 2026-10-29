package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.response.LecturerSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.LecturerWeeklyTimetableResponse;

public interface LecturerTimetableService {

  List<LecturerSemesterResponse> getAvailableSemesters(Long lecturerId);

  LecturerWeeklyTimetableResponse getWeeklyTimetable(
      Long lecturerId,
      Long semesterId,
      int weekNumber
  );
}