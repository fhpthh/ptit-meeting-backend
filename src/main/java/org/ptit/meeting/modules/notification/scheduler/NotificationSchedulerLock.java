package org.ptit.meeting.modules.notification.scheduler;

import java.time.Duration;

/**
 * Distributed lock contract preventing duplicate scheduler runs across multiple instances.
 */
public interface NotificationSchedulerLock {

  /**
   * Attempts to acquire a lock and returns a release token when acquired, or {@code null}
   * if another node currently holds the lock.
   */
  String tryAcquire(String lockKey, Duration ttl);

  void release(String lockKey, String token);
}
