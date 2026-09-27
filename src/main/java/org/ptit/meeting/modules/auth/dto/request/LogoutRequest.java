
package org.ptit.meeting.modules.auth.dto.request;

public record LogoutRequest(
    String refreshToken
) {}
