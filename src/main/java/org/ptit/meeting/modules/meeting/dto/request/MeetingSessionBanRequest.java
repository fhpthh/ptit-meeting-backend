package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.Size;

public record MeetingSessionBanRequest(
    @Size(max = 500, message = "{validation.meeting.moderation.ban-reason.size}")
    String reason
) {
}
