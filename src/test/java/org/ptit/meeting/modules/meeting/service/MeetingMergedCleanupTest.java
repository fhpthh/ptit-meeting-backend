package org.ptit.meeting.modules.meeting.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.modules.ai.service.AiRealtimeSessionService;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.impl.MeetingRuntimeSessionServiceImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class MeetingMergedCleanupTest {
  @Mock MeetingRuntimeSessionRepository runtimeSessionRepository;
  @Mock LiveKitGateway liveKitGateway;
  @Mock MeetingChatService chatService;
  @Mock MeetingHandRaiseService handRaiseService;
  @Mock MeetingHostPresenceService hostPresenceService;
  @Mock AiRealtimeSessionService aiRealtimeSessionService;
  @Mock PlatformTransactionManager transactionManager;
  @Mock TransactionStatus transactionStatus;
  @Mock MeetingSession session;
  @InjectMocks MeetingRuntimeSessionServiceImpl service;

  @BeforeEach
  void pendingEndedSession() {
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(runtimeSessionRepository.findByPublicIdForUpdate("session"))
        .thenReturn(Optional.of(session));
    when(session.getRuntimeStatus()).thenReturn(MeetingRuntimeSessionStatus.ENDED);
    when(session.isCleanupPending()).thenReturn(true);
    when(session.getProviderRoomName()).thenReturn("room");
  }

  @Test
  void closesMediaAndStopsAiBeforeCompletingRetryableCleanup() {
    service.cleanupEndedSession("session");
    var order = inOrder(liveKitGateway, aiRealtimeSessionService, chatService,
        handRaiseService, hostPresenceService, session);
    order.verify(liveKitGateway).deleteRoom("room");
    order.verify(aiRealtimeSessionService).stopAfterMeetingEnded("session");
    order.verify(chatService).clearHistory("session");
    order.verify(handRaiseService).clear("session");
    order.verify(hostPresenceService).clear("session");
    order.verify(session).setCleanupPending(false);
  }

  @Test
  void leavesCleanupPendingWhenProviderFails() {
    doThrow(new IllegalStateException("provider unavailable")).when(liveKitGateway).deleteRoom("room");
    assertThrows(IllegalStateException.class, () -> service.cleanupEndedSession("session"));
    verify(session, never()).setCleanupPending(false);
    verify(transactionManager).rollback(transactionStatus);
  }
}
