package org.ptit.meeting.modules.notification.service;

import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;

/**
 * Shared core publishing engine for all notification producers (PGV, Lecturer,
 * scheduled class reminders, and domain event listeners).
 */
public interface NotificationPublisherService {

  NotificationPublishResult publish(NotificationPublishCommand command);
}
