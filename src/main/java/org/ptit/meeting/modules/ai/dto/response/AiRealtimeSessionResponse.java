package org.ptit.meeting.modules.ai.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus;

public record AiRealtimeSessionResponse(
    String runtimeSessionId,
    long enableEpoch,
    AiRealtimeStatus status,
    Instant startedAt,
    Instant stoppedAt
) {
}
