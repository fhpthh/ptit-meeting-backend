package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;

public record ClassSectionStudentRow(
    Long enrollmentId,
    Long studentId,
    String studentCode,
    String fullName,
    String email,
    String managementClass,
    EnrollmentStatus status,
    LocalDateTime enrolledAt
) {
}
