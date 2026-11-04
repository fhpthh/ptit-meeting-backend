package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public record MeetingRoomMemberRoleUpdateRequest(
    @NotNull(message = "{validation.meeting.member.role.required}")
    MeetingRole role
) {
}
