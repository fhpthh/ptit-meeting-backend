package org.ptit.meeting.modules.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public record MeetingHandRaiseResponse(
    Long userId,
    String participantIdentity,
    String displayName,
    MeetingRole role,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant raisedAt,
    boolean raised
) {
}
