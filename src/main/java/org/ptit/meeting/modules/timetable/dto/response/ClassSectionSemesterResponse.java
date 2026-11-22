package org.ptit.meeting.modules.timetable.dto.response;

public record ClassSectionSemesterResponse(
    Long id,
    String name,
    String academicYear,
    Integer semesterNumber,
    boolean active
) {
}
