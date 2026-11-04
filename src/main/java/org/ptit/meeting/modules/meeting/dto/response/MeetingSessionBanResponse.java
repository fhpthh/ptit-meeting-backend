package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;

public record MeetingSessionBanResponse(
    String runtimeSessionId,
    Long userId,
    Long bannedByUserId,
    String reason,
    Instant bannedAt,
    boolean active,
    Instant revokedAt,
    Long revokedByUserId
) {
}
