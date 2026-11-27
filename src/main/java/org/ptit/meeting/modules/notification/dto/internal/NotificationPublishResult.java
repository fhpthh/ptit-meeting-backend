package org.ptit.meeting.modules.notification.dto.internal;

import org.ptit.meeting.modules.notification.entity.Notification;

public record NotificationPublishResult(
    Notification notification,
    long totalRecipients,
    boolean deduplicated
) {
}
