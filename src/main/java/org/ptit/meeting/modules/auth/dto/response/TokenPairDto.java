
package org.ptit.meeting.modules.auth.dto.response;

import lombok.Builder;

@Builder
public record TokenPairDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
