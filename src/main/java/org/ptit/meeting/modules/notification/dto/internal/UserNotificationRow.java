package org.ptit.meeting.modules.notification.dto.internal;

import java.time.Instant;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;

public record UserNotificationRow(
    String recipientId,
    String notificationId,
    NotificationType type,
    NotificationCategory category,
    NotificationPriority priority,
    String title,
    String content,
    String referenceType,
    String referenceId,
    Long createdBy,
    boolean read,
    Instant readAt,
    Instant publishedAt
) {
}
