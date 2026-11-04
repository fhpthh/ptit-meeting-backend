package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRecordingStatus;

public record MeetingRecordingResponse(
    String id,
    String runtimeSessionId,
    String meetingRoomId,
    MeetingRecordingStatus status,
    String url,
    Long sizeBytes,
    Long durationMs,
    String failureDetail,
    Long requestedByUserId,
    Instant startedAt,
    Instant endedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
