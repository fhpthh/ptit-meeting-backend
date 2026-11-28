package org.ptit.meeting.modules.timetable.dto.response;

/**
 * Lightweight active class section option used by cross-module audience selectors.
 */
public record ClassSectionOptionDto(
    Long id,
    String classCode,
    String subjectCode,
    String subjectName,
    String groupNumber,
    String lecturerName,
    long enrolledStudentCount
) {
}
