package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;

public record ClassSectionResponse(
    Long id,
    String classCode,
    String groupNumber,
    String practiceGroup,
    String managementClass,
    String subjectCode,
    String subjectName,
    Integer credits,
    Long semesterId,
    String semesterName,
    String academicYear,
    Integer semesterNumber,
    ClassSectionStatus status,
    long enrolledStudentCount,
    Integer expectedStudentCount,
    long totalSessions,
    long completedSessions,
    LocalDateTime nextSessionStart
) {
}
