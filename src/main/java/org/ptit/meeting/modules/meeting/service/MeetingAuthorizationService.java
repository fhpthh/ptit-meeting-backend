package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;

public interface MeetingAuthorizationService {

    ClassEnrollment requireActiveMember(String roomId, Long userId);

    void requireRoomOwner(String roomId, Long userId);

    void requireRuntimeSessionHost(String roomId, Long userId);
}
