package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalTime;

public record ClassSectionSummaryResponse(
    Long id,
    String classCode,
    String groupNumber,
    String practiceGroup,
    Long lecturerId,
    String lecturerName,
    String status,
    long enrolledStudentCount,
    Integer expectedStudentCount,
    ScheduleSummary primarySchedule,
    OnlineRoomSummary onlineRoom
) {
  public record ScheduleSummary(
      Long id,
      Integer dayOfWeek,
      Integer startPeriod,
      Integer numberOfPeriods,
      LocalTime startTime,
      LocalTime endTime,
      String room,
      String building,
      boolean online
  ) {
  }

  public record OnlineRoomSummary(
      Long id,
      String name,
      String status
  ) {
  }
}
