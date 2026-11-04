package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MeetingRecordingCompleteRequest(
    @NotNull(message = "{validation.meeting.recording.duration.required}")
    @PositiveOrZero(message = "{validation.meeting.recording.duration.invalid}")
    Long durationMs
) {
}
