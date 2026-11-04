package org.ptit.meeting.modules.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public record MeetingChatMessageResponse(
    String messageId,
    UUID clientMessageId,
    String cursor,
    Long senderUserId,
    String senderIdentity,
    String senderName,
    MeetingRole senderRole,
    String content,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant sentAt
) {
}
