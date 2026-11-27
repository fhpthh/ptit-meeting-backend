package org.ptit.meeting.modules.notification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.service.OnlineClassReminderService;

@ExtendWith(MockitoExtension.class)
class OnlineClassReminderSchedulerTest {

  @Mock
  private OnlineClassReminderService reminderService;
  @Mock
  private NotificationSchedulerLock schedulerLock;

  private NotificationProperties properties;
  private OnlineClassReminderScheduler scheduler;

  @BeforeEach
  void setUp() {
    properties = new NotificationProperties();
    properties.setReminderEnabled(true);
    properties.setReminderLockKey("lock:test");
    properties.setReminderLockTtlMs(55_000L);
    scheduler = new OnlineClassReminderScheduler(reminderService, schedulerLock, properties);
  }

  @Test
  void scanUpcomingOnlineClasses_whenDisabled_doesNothing() {
    properties.setReminderEnabled(false);

    scheduler.scanUpcomingOnlineClasses();

    verify(schedulerLock, never()).tryAcquire(any(), any());
    verify(reminderService, never()).processUpcomingOnlineSessionReminders();
  }

  @Test
  void scanUpcomingOnlineClasses_whenLockNotAcquired_skipsProcessing() {
    when(schedulerLock.tryAcquire(eq("lock:test"), eq(Duration.ofMillis(55_000L))))
        .thenReturn(null);

    scheduler.scanUpcomingOnlineClasses();

    verify(reminderService, never()).processUpcomingOnlineSessionReminders();
    verify(schedulerLock, never()).release(any(), any());
  }

  @Test
  void scanUpcomingOnlineClasses_whenLockAcquired_processesAndReleasesLock() {
    when(schedulerLock.tryAcquire(eq("lock:test"), eq(Duration.ofMillis(55_000L))))
        .thenReturn("tok-1");

    scheduler.scanUpcomingOnlineClasses();

    verify(reminderService).processUpcomingOnlineSessionReminders();
    verify(schedulerLock).release("lock:test", "tok-1");
  }
}
