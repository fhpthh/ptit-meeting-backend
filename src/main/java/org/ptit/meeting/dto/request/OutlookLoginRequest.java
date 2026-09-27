package org.ptit.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OutlookLoginRequest(
    @NotBlank(message = "Authorization code không được để trống")
    String authorizationCode,

    @NotBlank(message = "Redirect URI không được để trống")
    String redirectUrl
) {}
