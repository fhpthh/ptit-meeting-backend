package org.ptit.meeting.modules.notification.scheduler;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.service.NotificationRealtimeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically sends SSE heartbeat events to prevent idle proxy timeouts
 * and clean up stale client connections.
 */
@Component
@RequiredArgsConstructor
public class NotificationSseHeartbeatScheduler {

  private final NotificationRealtimeService realtimeService;
  private final NotificationProperties properties;

  @Scheduled(fixedDelayString = "${application.notification.sse-heartbeat-interval-ms:25000}")
  public void sendHeartbeat() {
    if (!properties.isSseEnabled()) {
      return;
    }
    realtimeService.sendHeartbeat();
  }
}
