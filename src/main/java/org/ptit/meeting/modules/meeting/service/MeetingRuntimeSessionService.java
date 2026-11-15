package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;

import java.util.List;

public interface MeetingRuntimeSessionService {

  MeetingRuntimeSessionResponse create(
      String roomId,
      MeetingRuntimeSessionCreateRequest request,
      Long currentUserId
  );

  List<MeetingRuntimeSessionResponse> getByRoom(String roomId, Long currentUserId);

  MeetingRuntimeSessionResponse getById(String runtimeSessionId, Long currentUserId);

  MeetingRuntimeSessionResponse start(String runtimeSessionId, Long currentUserId);

  MeetingRuntimeSessionResponse end(String runtimeSessionId, Long currentUserId);

  MeetingRuntimeSessionResponse cancel(String runtimeSessionId, Long currentUserId);

  void reconcileSessions();

  void reconcileOvertime(
      String runtimeSessionId,
      Long observedVersion,
      String observedHostState,
      List<LiveKitParticipantSnapshot> participants
  );

  void cleanupEndedSession(String runtimeSessionId);
}
