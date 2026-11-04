package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;

public record ClassSectionStudentResponse(
    Long enrollmentId,
    Long studentId,
    String studentCode,
    String fullName,
    String email,
    String managementClass,
    String status,
    LocalDateTime enrolledAt
) {
}
