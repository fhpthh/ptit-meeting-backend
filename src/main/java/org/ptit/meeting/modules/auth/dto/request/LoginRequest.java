
package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "{validation.auth.username.required}")
    String username,

    @NotBlank(message = "{validation.auth.password.required}")
    String password
) {}
