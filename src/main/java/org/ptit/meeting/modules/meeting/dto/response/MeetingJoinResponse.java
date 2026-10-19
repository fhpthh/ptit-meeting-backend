package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public record MeetingJoinResponse(
    String sessionId,
    String roomId,
    String websocketUrl,
    String participantToken,
    Instant tokenExpiresAt,
    String participantIdentity,
    MeetingRole role,
    boolean canSubscribe,
    boolean canPublishMicrophone,
    boolean canPublishCamera,
    boolean canShareScreen
) {
}
