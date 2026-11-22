package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.meeting.constant.enums.ParticipantConnectionStatus;

public record MeetingAttendanceConnectionResponse(
    String id,
    Instant joinedAt,
    Instant leftAt,
    long durationSeconds,
    ParticipantConnectionStatus status,
    boolean currentlyOnline
) {
}
