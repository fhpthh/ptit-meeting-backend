package org.ptit.meeting.integration.livekit.dto;

import java.time.Instant;

public record LiveKitTokenResult(
    String websocketUrl,
    String participantToken,
    Instant expiresAt
) {
}
