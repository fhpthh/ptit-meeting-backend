package org.ptit.meeting.integration.livekit.dto;

import java.util.Set;

public record LiveKitParticipantSnapshot(
    String sid,
    String identity,
    String metadata,
    long joinedAt,
    boolean canSubscribe,
    Set<String> publishSources
) {
}
