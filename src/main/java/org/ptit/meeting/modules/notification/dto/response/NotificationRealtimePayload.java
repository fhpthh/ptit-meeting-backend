package org.ptit.meeting.modules.notification.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;

/**
 * Payload pushed over SSE when a new in-app notification is published for a user.
 */
public record NotificationRealtimePayload(
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
    Instant publishedAt
) {
}
