package org.ptit.meeting.modules.meeting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.meeting.event.MeetingParticipantBannedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingParticipantBannedListener {

  private final LiveKitGateway liveKitGateway;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void removeBannedParticipant(MeetingParticipantBannedEvent event) {
    try {
      liveKitGateway.removeParticipant(
          event.providerRoomName(),
          event.participantIdentity()
      );
    } catch (LiveKitIntegrationException exception) {
      log.warn(
          "Unable to remove newly banned LiveKit participant: roomName={}, identity={}",
          event.providerRoomName(),
          event.participantIdentity()
      );
    }
  }
}
