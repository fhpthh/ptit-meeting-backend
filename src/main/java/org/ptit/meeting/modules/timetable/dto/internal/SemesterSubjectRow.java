package org.ptit.meeting.modules.timetable.dto.internal;

public record SemesterSubjectRow(
    Long subjectId,
    String subjectCode,
    String subjectName,
    Integer credit,
    String department,
    long classSectionCount
) {
}
