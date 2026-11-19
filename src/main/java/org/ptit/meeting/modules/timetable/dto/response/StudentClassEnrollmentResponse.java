package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;

public record StudentClassEnrollmentResponse(
    Long id, Long sectionId, String classCode, Long semesterId, String semesterCode,
    String semesterName, String subjectCode, String subjectName, String groupNumber,
    String practiceGroup, EnrollmentStatus status, LocalDateTime enrolledAt
) {}
