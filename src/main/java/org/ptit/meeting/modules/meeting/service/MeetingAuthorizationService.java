package org.ptit.meeting.modules.meeting.service;

public interface MeetingAuthorizationService {

    void requireActiveMember(String roomId, Long userId);

    void requireRoomOwner(String roomId, Long userId);

    void requireSessionHost(String roomId, Long userId);
}
