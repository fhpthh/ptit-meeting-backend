package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.ptit.meeting.modules.timetable.constant.enums.SessionStatus;

public record LecturerWeeklyTimetableResponse(
    LecturerSemesterResponse semester,
    WeekInfo week,
    TimelineInfo timeline,
    List<TimetableItem> items
) {

  public record WeekInfo(
      int weekNumber,
      LocalDate startDate,
      LocalDate endDate
  ) {
  }

  public record TimelineInfo(
      LocalTime startTime,
      LocalTime endTime
  ) {
  }

  public record TimetableItem(
      Long sessionId,
      Long scheduleId,
      Long classSectionId,

      LocalDate teachingDate,
      Integer dayOfWeek,

      Integer startPeriod,
      Integer numberOfPeriods,
      LocalTime startTime,
      LocalTime endTime,

      SubjectInfo subject,
      ClassSectionInfo classSection,
      LecturerInfo lecturer,
      RoomInfo room,

      SessionStatus status
  ) {
  }

  public record SubjectInfo(
      String code,
      String name
  ) {
  }

  public record ClassSectionInfo(
      String code,
      String groupNumber,
      String practiceGroup,
      String managementClass
  ) {
  }

  public record LecturerInfo(
      Long id,
      String name
  ) {
  }

  public record RoomInfo(
      Long id,
      String name
  ) {
  }
}