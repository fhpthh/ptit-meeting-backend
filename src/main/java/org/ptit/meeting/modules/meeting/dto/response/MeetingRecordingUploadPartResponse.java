package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;

public record MeetingRecordingUploadPartResponse(
    int partNumber,
    String url,
    Instant expiresAt
) {
}
