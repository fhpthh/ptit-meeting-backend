package org.ptit.meeting.modules.meeting.scheduler;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRuntimeSessionScheduler {

  private final MeetingRuntimeSessionService runtimeSessionService;
  private final MeetingJoinService joinService;

  @Scheduled(fixedDelay = 30_000)
  public void reconcileSessions() {
    runtimeSessionService.reconcileSessions();
    joinService.reconcileActiveSessions();
  }
}
