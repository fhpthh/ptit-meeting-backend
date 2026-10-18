package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionScheduleSyncRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionResponse;

import java.util.List;

public interface MeetingSessionService {

  MeetingSessionResponse create(
      String roomId,
      MeetingSessionCreateRequest request,
      Long currentUserId
  );

  MeetingSessionResponse syncFromSchedule(MeetingSessionScheduleSyncRequest request);

  List<MeetingSessionResponse> getByRoom(String roomId, Long currentUserId);

  MeetingSessionResponse getById(String sessionId, Long currentUserId);

  MeetingSessionResponse start(String sessionId, Long currentUserId);

  MeetingSessionResponse end(String sessionId, Long currentUserId);

  MeetingSessionResponse cancel(String sessionId, Long currentUserId);
}
