package org.ptit.meeting.modules.meeting.dto.response;

import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;

import java.time.Instant;

public record MeetingRoomMemberResponse(
    String id,
    String roomId,
    Long userId,
    MeetingRole role,
    MemberStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
