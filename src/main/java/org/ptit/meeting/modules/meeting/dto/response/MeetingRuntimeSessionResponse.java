package org.ptit.meeting.modules.meeting.dto.response;

import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;

import java.time.Instant;

public record MeetingRuntimeSessionResponse(
    String id,
    String meetingRoomId,
    String title,
    String scheduleOccurrenceId,
    Instant scheduledStartAt,
    Instant scheduledEndAt,
    Instant actualStartAt,
    Instant actualEndAt,
    MeetingRuntimeSessionStatus status,
    Long createdByUserId,
    Instant createdAt,
    Instant updatedAt
) {
}
