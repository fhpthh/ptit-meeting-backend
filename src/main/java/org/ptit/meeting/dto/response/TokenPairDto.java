package org.ptit.meeting.dto.response;

import lombok.Builder;

@Builder
public record TokenPairDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
