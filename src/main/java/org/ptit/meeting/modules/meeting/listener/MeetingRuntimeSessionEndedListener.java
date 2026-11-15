package org.ptit.meeting.modules.meeting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.meeting.event.MeetingRuntimeSessionEndedEvent;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRuntimeSessionEndedListener {

  private final MeetingRuntimeSessionService runtimeSessionService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void cleanupRuntimeSession(MeetingRuntimeSessionEndedEvent event) {
    try {
      runtimeSessionService.cleanupEndedSession(event.runtimeSessionId());
    } catch (RuntimeException exception) {
      log.warn(
          "Meeting cleanup will retry after committed session end: runtimeSessionId={}",
          event.runtimeSessionId(),
          exception
      );
    }
  }
}
