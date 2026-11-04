package org.ptit.meeting.modules.meeting.dto.internal;

import java.time.Instant;

public record ParticipantConnectionEvent(
    String runtimeSessionId,
    Long userId,
    String participantIdentity,
    String participantSid,
    Instant occurredAt,
    String disconnectReason
) {
}
