package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDate;

public record StudentSemesterResponse(
    Long id,
    String code,
    String name,
    String academicYear,
    Integer semesterNumber,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    long totalWeeks
) {
}