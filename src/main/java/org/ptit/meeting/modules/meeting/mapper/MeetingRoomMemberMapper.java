package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomMemberMapper {

  public MeetingRoomMemberResponse toResponse(MeetingRoomMember member) {
    return new MeetingRoomMemberResponse(
        member.getId(),
        member.getRoomId(),
        member.getUserId(),
        member.getRole(),
        member.getStatus(),
        member.getCreatedAt(),
        member.getUpdatedAt()
    );
  }
}
