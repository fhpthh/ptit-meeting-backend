package org.ptit.meeting.modules.timetable.dto.response;

public record SemesterSubjectResponse(
    Long subjectId,
    String subjectCode,
    String subjectName,
    Integer credit,
    Long semesterId,
    String semesterCode,
    String semesterName,
    String department,
    String cohort,
    long classSectionCount
) {
}
