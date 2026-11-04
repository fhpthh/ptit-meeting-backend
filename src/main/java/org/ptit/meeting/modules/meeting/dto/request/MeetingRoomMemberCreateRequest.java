package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public record MeetingRoomMemberCreateRequest(
    @NotNull(message = "{validation.meeting.member.user-id.required}")
    @Positive(message = "{validation.meeting.member.user-id.positive}")
    Long userId,

    @NotNull(message = "{validation.meeting.member.role.required}")
    MeetingRole role
) {
}
