package org.ptit.meeting.modules.timetable.dto.internal;

import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;

public record ClassSectionListRow(
    Long id,
    Long semesterId,
    Long subjectId,
    Long lecturerId,
    String lecturerName,
    String cohort,
    String classCode,
    String groupNumber,
    String practiceGroup,
    String managementClass,
    ClassSectionStatus status,
    Integer expectedStudentCount,
    Long enrolledStudentCount
) {
}
