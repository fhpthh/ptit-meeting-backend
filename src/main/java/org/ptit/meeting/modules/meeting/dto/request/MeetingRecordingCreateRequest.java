package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MeetingRecordingCreateRequest(
    @NotBlank(message = "{validation.meeting.recording.mime-type.required}")
    @Size(max = 100, message = "{validation.meeting.recording.mime-type.invalid}")
    @Pattern(
        regexp = "^video/webm(?:;.*)?$",
        message = "{validation.meeting.recording.mime-type.invalid}"
    )
    String mimeType
) {
}
