package org.ptit.meeting.modules.meeting.dto.request;

import java.time.Instant;

public record MeetingSessionScheduleSyncRequest(
    String meetingRoomId,
    String scheduleOccurrenceId,
    String title,
    Instant scheduledStartAt,
    Instant scheduledEndAt,
    Long createdByUserId
) {
}
