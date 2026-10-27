package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionBanRequest;
import org.ptit.meeting.modules.meeting.dto.request.ParticipantTrackMuteRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanListItemResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanResponse;

public interface MeetingModerationService {

  void muteTrack(
      String runtimeSessionId,
      Long participantUserId,
      ParticipantTrackMuteRequest request,
      Long currentUserId
  );

  void kick(
      String runtimeSessionId,
      Long participantUserId,
      Long currentUserId
  );

  MeetingSessionBanResponse ban(
      String runtimeSessionId,
      Long participantUserId,
      MeetingSessionBanRequest request,
      Long currentUserId
  );

  MeetingSessionBanResponse unban(
      String runtimeSessionId,
      Long participantUserId,
      Long currentUserId
  );

  List<MeetingSessionBanListItemResponse> getActiveBans(
      String runtimeSessionId,
      Long currentUserId
  );

  void requireNotBanned(String runtimeSessionId, Long userId);

  boolean enforceBan(String runtimeSessionId, Long userId);
}
