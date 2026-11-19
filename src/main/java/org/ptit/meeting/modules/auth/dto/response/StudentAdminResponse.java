package org.ptit.meeting.modules.auth.dto.response;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;

public record StudentAdminResponse(
    Long id, String code, String fullName, String email, String department, UserStatus status,
    boolean mustChangePassword, LocalDateTime createdAt, LocalDateTime updatedAt,
    StudentAcademicProfileResponse academicProfile
) {}
