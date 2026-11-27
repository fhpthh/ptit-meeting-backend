package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;

public interface MeetingAuthorizationService {

    MeetingRole requireRoomAccess(String roomId, Long userId);

    ClassEnrollment requireActiveMember(String roomId, Long userId);

    void requireRoomOwner(String roomId, Long userId);

    void requireRuntimeSessionHost(String roomId, Long userId);
}
