package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import java.util.List;

public interface LiveKitGateway {

  LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant);

  void ensureRoom(String roomName, int maxParticipants);

  List<LiveKitParticipantSnapshot> listParticipants(String roomName);

  boolean updateParticipant(LiveKitJoinGrant grant);

  void deleteRoom(String roomName);

  boolean mutePublishedTrack(
      String roomName,
      String participantIdentity,
      String trackSid
  );

  boolean removeParticipant(String roomName, String participantIdentity);

  void broadcastReliableData(String roomName, byte[] data, String topic);

}
