
package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordFirstTimeRequest(
    @NotNull(message = "{validation.auth.user-id.required}")
    Long userId,

    @NotBlank(message = "{validation.auth.old-password.required}")
    String oldPassword,

    @NotBlank(message = "{validation.auth.new-password.required}")
    @Size(min = 8, message = "{validation.auth.new-password.size}")
    String newPassword,

    @NotBlank(message = "{validation.auth.confirm-new-password.required}")
    String confirmNewPassword
) {}
