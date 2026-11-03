package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SemesterResponse(
    Long id,
    String code,
    String name,
    String academicYear,
    Integer semesterNum,
    LocalDate startDate,
    LocalDate endDate,
    boolean isActive,
    long totalWeeks,
    long classSectionCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
