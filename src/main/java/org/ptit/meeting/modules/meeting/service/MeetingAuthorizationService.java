package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;

public interface MeetingAuthorizationService {

    MeetingRoomMember requireActiveMember(String roomId, Long userId);

    void requireRoomOwner(String roomId, Long userId);

    void requireSessionHost(String roomId, Long userId);
}
