package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;

public interface LiveKitGateway {

  LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant);

  void deleteRoom(String roomName);

  boolean mutePublishedTrack(
      String roomName,
      String participantIdentity,
      String trackSid
  );

  boolean removeParticipant(String roomName, String participantIdentity);

  void broadcastReliableData(String roomName, byte[] data, String topic);
}
