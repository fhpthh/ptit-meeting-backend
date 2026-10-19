package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.meeting.dto.response.MeetingJoinResponse;

public interface MeetingJoinService {

  MeetingJoinResponse join(String runtimeSessionId, Long currentUserId);
}
