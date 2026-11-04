package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;

public record MeetingAttendanceResponse(
    Long userId,
    Instant firstJoinedAt,
    Instant lastLeftAt,
    long totalDurationSeconds,
    int connectionCount,
    boolean currentlyOnline
) {
}
