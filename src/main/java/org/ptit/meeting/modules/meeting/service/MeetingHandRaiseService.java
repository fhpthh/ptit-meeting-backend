package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.response.MeetingHandRaiseResponse;

public interface MeetingHandRaiseService {

  MeetingHandRaiseResponse raise(String runtimeSessionId, Long currentUserId);

  void lower(String runtimeSessionId, Long currentUserId);

  List<MeetingHandRaiseResponse> getRaisedHands(
      String runtimeSessionId,
      Long currentUserId
  );

  void removeDisconnected(
      String runtimeSessionId,
      String providerRoomName,
      Long userId
  );

  void clear(String runtimeSessionId);
}
