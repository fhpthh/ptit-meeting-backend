package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;

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

  /** Reconcile a LiveKit snapshot; authorized AI workers do not count as participants. */
  void reconcileOvertime(
      String runtimeSessionId,
      Long observedVersion,
      boolean hasParticipants
  );

  void cleanupEndedSession(String runtimeSessionId);
}
