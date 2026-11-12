package org.ptit.meeting.modules.meeting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.meeting.event.MeetingParticipantRemovedEvent;
import org.ptit.meeting.modules.meeting.event.MeetingRoomAccessChangedEvent;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingParticipantAccessListener {

  private final LiveKitGateway liveKitGateway;
  private final MeetingJoinService joinService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void synchronizeAccess(MeetingRoomAccessChangedEvent event) {
    try {
      joinService.synchronizeRoom(event.roomId());
    } catch (LiveKitIntegrationException exception) {
      log.warn("Unable to synchronize LiveKit access; reconciliation will retry: roomId={}", event.roomId());
    }
  }

  @Scheduled(initialDelay = 60_000, fixedDelay = 30_000)
  public void reconcileAccess() {
    joinService.reconcileActiveSessions();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void removeParticipant(MeetingParticipantRemovedEvent event) {
    try {
      liveKitGateway.removeParticipant(
          event.providerRoomName(),
          event.participantIdentity()
      );
    } catch (LiveKitIntegrationException exception) {
      log.warn(
          "Unable to remove LiveKit participant after access revocation: roomName={}, identity={}",
          event.providerRoomName(),
          event.participantIdentity()
      );
    }
  }
}
