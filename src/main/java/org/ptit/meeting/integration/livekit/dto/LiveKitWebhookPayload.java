package org.ptit.meeting.integration.livekit.dto;

import java.time.Instant;

public record LiveKitWebhookPayload(
    String eventId,
    String eventType,
    Instant createdAt,
    String roomName,
    String participantSid,
    String participantIdentity,
    String participantMetadata,
    String disconnectReason
) {
}
