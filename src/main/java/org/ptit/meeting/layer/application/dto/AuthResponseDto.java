package org.ptit.meeting.layer.application.dto;

import lombok.Builder;

@Builder
public record AuthResponseDto(
    TokenPairDto token,
    UserProfileDto user,
    boolean mustChangePassword
) {

}
