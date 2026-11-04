package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MeetingChatMessageRequest(
    @NotNull(message = "{validation.meeting.chat.client-message-id.required}")
    UUID clientMessageId,

    @NotBlank(message = "{validation.meeting.chat.content.required}")
    @Size(max = 1000, message = "{validation.meeting.chat.content.size}")
    String content
) {
}
