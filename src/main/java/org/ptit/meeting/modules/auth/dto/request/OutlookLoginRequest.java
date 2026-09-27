
package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OutlookLoginRequest(
    @NotBlank(message = "{validation.auth.authorization-code.required}")
    String authorizationCode,

    @NotBlank(message = "{validation.auth.redirect-uri.required}")
    String redirectUrl
) {}
