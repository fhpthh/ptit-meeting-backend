package org.ptit.meeting.modules.timetable.mapper;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.timetable.dto.internal.StudentOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentTimetableMapper {

  private final ScheduleDateCalculator dateCalculator;

  public StudentSemesterResponse toSemesterResponse(Semester semester) {
    return new StudentSemesterResponse(
        semester.getId(),
        semester.getCode(),
        semester.getName(),
        semester.getAcademicYear(),
        semester.getSemesterNum(),
        semester.getStartDate(),
        semester.getEndDate(),
        semester.isActive(),
        calculateTotalWeeks(semester)
    );
  }

  public StudentWeeklyTimetableResponse.TimetableItem toTimetableItem(
      StudentOnlineScheduleRow row
  ) {
    return new StudentWeeklyTimetableResponse.TimetableItem(
        row.sessionId(),
        row.scheduleId(),
        row.classSectionId(),

        row.studyDate(),
        row.studyDate().getDayOfWeek().getValue() + 1,

        row.startPeriod(),
        row.numberOfPeriods(),
        row.scheduledStart().toLocalTime(),
        row.scheduledEnd().toLocalTime(),

        new StudentWeeklyTimetableResponse.SubjectInfo(
            row.subjectCode(),
            row.subjectName()
        ),

        new StudentWeeklyTimetableResponse.ClassSectionInfo(
            row.classCode(),
            row.groupNumber(),
            row.practiceGroup()
        ),

        new StudentWeeklyTimetableResponse.LecturerInfo(
            row.lecturerId(),
            row.lecturerName()
        ),

        new StudentWeeklyTimetableResponse.RoomInfo(
            row.roomId(),
            row.roomName()
        ),

        row.sessionStatus()
    );
  }

  private long calculateTotalWeeks(Semester semester) {
    return dateCalculator.calculateTotalWeeks(
        semester.getStartDate(),
        semester.getEndDate()
    );
  }
}
