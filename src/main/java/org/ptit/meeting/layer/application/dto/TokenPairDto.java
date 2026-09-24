package org.ptit.meeting.layer.application.dto;

import lombok.Builder;

@Builder
public record TokenPairDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {

}
