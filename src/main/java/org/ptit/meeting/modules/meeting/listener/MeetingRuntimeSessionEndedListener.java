package org.ptit.meeting.modules.meeting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.meeting.event.MeetingRuntimeSessionEndedEvent;
import org.ptit.meeting.modules.meeting.service.MeetingChatService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRuntimeSessionEndedListener {

  private final LiveKitGateway liveKitGateway;
  private final MeetingChatService chatService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void cleanupRuntimeSession(MeetingRuntimeSessionEndedEvent event) {
    chatService.clearHistory(event.runtimeSessionId());
    try {
      liveKitGateway.deleteRoom(event.providerRoomName());
    } catch (LiveKitIntegrationException exception) {
      log.warn(
          "Unable to close LiveKit room after ending runtime session: runtimeSessionId={}, roomName={}",
          event.runtimeSessionId(),
          event.providerRoomName()
      );
    }
  }
}
