package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalTime;
import java.util.List;

public record SemesterSubjectDetailResponse(
    SemesterSubjectResponse subject,
    List<ClassSectionDetail> classSections
) {
  public record ClassSectionDetail(
      Long id,
      String classCode,
      String groupNumber,
      String practiceGroup,
      String cohort,
      String managementClass,
      String lecturerName,
      String status,
      List<ScheduleDetail> schedules
  ) {
  }

  public record ScheduleDetail(
      Long id,
      Integer dayOfWeek,
      Integer startPeriod,
      Integer numberOfPeriods,
      LocalTime startTime,
      LocalTime endTime,
      String weeksPattern,
      String room,
      String building,
      boolean online
  ) {
  }
}
