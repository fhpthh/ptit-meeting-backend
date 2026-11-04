package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantTrackMuteRequest(
    @NotBlank(message = "{validation.meeting.moderation.track-sid.required}")
    @Size(max = 128, message = "{validation.meeting.moderation.track-sid.size}")
    String trackSid
) {
}
