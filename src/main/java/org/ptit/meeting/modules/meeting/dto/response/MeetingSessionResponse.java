package org.ptit.meeting.modules.meeting.dto.response;

import org.ptit.meeting.modules.meeting.constant.enums.MeetingSessionStatus;

import java.time.Instant;

public record MeetingSessionResponse(
    String id,
    String meetingRoomId,
    String title,
    String scheduleOccurrenceId,
    Instant scheduledStartAt,
    Instant scheduledEndAt,
    Instant actualStartAt,
    Instant actualEndAt,
    MeetingSessionStatus status,
    Long createdByUserId,
    Instant createdAt,
    Instant updatedAt
) {
}
