package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberRoleUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;

import java.util.List;

public interface MeetingRoomMemberService {

  MeetingRoomMemberResponse add(
      String roomId,
      MeetingRoomMemberCreateRequest request,
      Long currentUserId
  );

  List<MeetingRoomMemberResponse> getActiveMembers(String roomId, Long currentUserId);

  MeetingRoomMemberResponse updateRole(
      String roomId,
      Long memberUserId,
      MeetingRoomMemberRoleUpdateRequest request,
      Long currentUserId
  );

  void remove(String roomId, Long memberUserId, Long currentUserId);
}
