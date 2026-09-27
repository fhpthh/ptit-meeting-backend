package org.ptit.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "validation.invalid_input")
    String refreshToken
) {}
