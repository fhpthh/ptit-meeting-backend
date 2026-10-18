package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MeetingSessionCreateRequest(
    @NotBlank(message = "{validation.meeting.session.title.required}")
    @Size(max = 150, message = "{validation.meeting.session.title.size}")
    String title,

    @NotNull(message = "{validation.meeting.session.start.required}")
    Instant scheduledStartAt,

    @NotNull(message = "{validation.meeting.session.end.required}")
    Instant scheduledEndAt
) {
}
