package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalTime;
import java.util.List;

public record ClassSectionDetailResponse(
    Long id,
    Long semesterId,
    String semesterCode,
    String semesterName,
    Long subjectId,
    String subjectCode,
    String subjectName,
    Integer credit,
    String department,
    String classCode,
    String groupNumber,
    String practiceGroup,
    String cohort,
    String managementClass,
    Long lecturerId,
    String lecturerName,
    String status,
    long enrolledStudentCount,
    Integer expectedStudentCount,
    ClassSectionSummaryResponse.OnlineRoomSummary onlineRoom,
    List<ScheduleDetail> schedules
) {
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
