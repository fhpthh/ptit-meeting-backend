package org.ptit.meeting.modules.timetable.dto.response;

public record AcademicDepartmentStatResponse(
    String code,
    String name,
    long classCount,
    String percentage,
    String color
) {
}
