package org.ptit.meeting.modules.notification.scheduler;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.service.OnlineClassReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineClassReminderScheduler {

  private final OnlineClassReminderService reminderService;
  private final NotificationSchedulerLock schedulerLock;
  private final NotificationProperties properties;

  @Scheduled(fixedDelayString = "${application.notification.reminder-scan-interval-ms:60000}")
  public void scanUpcomingOnlineClasses() {
    if (!properties.isReminderEnabled()) {
      return;
    }

    String lockKey = properties.getReminderLockKey();
    Duration ttl = Duration.ofMillis(properties.getReminderLockTtlMs());
    String token = schedulerLock.tryAcquire(lockKey, ttl);
    if (token == null) {
      return;
    }

    try {
      reminderService.processUpcomingOnlineSessionReminders();
    } catch (Exception ex) {
      log.error("Online class reminder scheduler failed", ex);
    } finally {
      schedulerLock.release(lockKey, token);
    }
  }
}
