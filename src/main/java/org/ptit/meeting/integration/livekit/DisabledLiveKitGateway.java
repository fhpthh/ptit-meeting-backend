package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;

final class DisabledLiveKitGateway implements LiveKitGateway {

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
