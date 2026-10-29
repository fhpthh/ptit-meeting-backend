
package org.ptit.meeting.modules.timetable.mapper;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.timetable.dto.internal.LecturerOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.response.LecturerSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.LecturerWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LecturerTimetableMapper {

  private final ScheduleDateCalculator dateCalculator;

  public LecturerSemesterResponse toSemesterResponse(Semester semester) {
    return new LecturerSemesterResponse(
        semester.getId(),
        semester.getCode(),
        semester.getName(),
        semester.getAcademicYear(),
        semester.getSemesterNum(),
        semester.getStartDate(),
        semester.getEndDate(),
        semester.isActive(),
        dateCalculator.calculateTotalWeeks(
            semester.getStartDate(),
            semester.getEndDate()
        )
    );
  }

  public LecturerWeeklyTimetableResponse.TimetableItem toTimetableItem(
      LecturerOnlineScheduleRow row
  ) {
    return new LecturerWeeklyTimetableResponse.TimetableItem(
        row.sessionId(),
        row.scheduleId(),
        row.classSectionId(),

        row.studyDate(),
        row.studyDate().getDayOfWeek().getValue() + 1,

        row.startPeriod(),
        row.numberOfPeriods(),
        row.scheduledStart().toLocalTime(),
        row.scheduledEnd().toLocalTime(),

        new LecturerWeeklyTimetableResponse.SubjectInfo(
            row.subjectCode(),
            row.subjectName()
        ),

        new LecturerWeeklyTimetableResponse.ClassSectionInfo(
            row.classCode(),
            row.groupNumber(),
            row.practiceGroup(),
            row.managementClass()
        ),

        new LecturerWeeklyTimetableResponse.LecturerInfo(
            row.lecturerId(),
            row.lecturerName()
        ),

        new LecturerWeeklyTimetableResponse.RoomInfo(
            row.roomId(),
            row.roomName()
        ),

        row.sessionStatus()
    );
  }
}