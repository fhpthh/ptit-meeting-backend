package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;

public record LecturerAdminStatusRequest(
    @NotNull UserStatus status
) {}
