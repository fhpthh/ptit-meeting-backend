package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;

public record StudentAdminStatusRequest(
    @NotNull(message = "{validation.student-admin.required}") UserStatus status
) {}
