package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.response.AiRealtimeSessionResponse;

public interface AiRealtimeSessionService {
  AiRealtimeSessionResponse start(String runtimeSessionId, Long currentUserId);
  AiRealtimeSessionResponse stop(String runtimeSessionId, Long currentUserId);
  AiRealtimeSessionResponse status(String runtimeSessionId, Long currentUserId);
  void stopAfterMeetingEnded(String runtimeSessionId);
}
