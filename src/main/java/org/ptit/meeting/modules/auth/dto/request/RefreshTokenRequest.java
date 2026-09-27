
package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "{validation.auth.refresh-token.required}")
    String refreshToken
) {}
