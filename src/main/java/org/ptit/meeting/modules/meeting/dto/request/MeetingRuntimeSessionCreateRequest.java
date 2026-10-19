package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MeetingRuntimeSessionCreateRequest(
    @NotBlank(message = "{validation.meeting.runtime-session.title.required}")
    @Size(max = 150, message = "{validation.meeting.runtime-session.title.size}")
    String title,

    @NotNull(message = "{validation.meeting.runtime-session.start.required}")
    Instant scheduledStartAt,

    @NotNull(message = "{validation.meeting.runtime-session.end.required}")
    Instant scheduledEndAt
) {
}
