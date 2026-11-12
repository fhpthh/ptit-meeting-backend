package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import java.util.List;

final class DisabledLiveKitGateway implements LiveKitGateway {

  @Override
  public void ensureRoom(String roomName, int maxParticipants) {
    throw disabledException();
  }

  @Override
  public List<LiveKitParticipantSnapshot> listParticipants(String roomName) {
    throw disabledException();
  }

  @Override
  public boolean updateParticipant(LiveKitJoinGrant grant) {
    throw disabledException();
  }

  @Override
  public LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant) {
    throw disabledException();
  }

  @Override
  public void deleteRoom(String roomName) {
    throw disabledException();
  }

  @Override
  public boolean mutePublishedTrack(
      String roomName,
      String participantIdentity,
      String trackSid
  ) {
    throw disabledException();
  }

  @Override
  public boolean removeParticipant(String roomName, String participantIdentity) {
    throw disabledException();
  }

  @Override
  public void broadcastReliableData(String roomName, byte[] data, String topic) {
    throw disabledException();
  }

  private LiveKitIntegrationException disabledException() {
    return new LiveKitIntegrationException("LiveKit integration is disabled");
  }
}
