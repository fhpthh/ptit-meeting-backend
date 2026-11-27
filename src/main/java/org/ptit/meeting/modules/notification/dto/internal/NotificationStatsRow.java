package org.ptit.meeting.modules.notification.dto.internal;

import java.time.Instant;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;

public record NotificationStatsRow(
    String id,
    NotificationType type,
    NotificationCategory category,
    NotificationPriority priority,
    String title,
    String content,
    TargetScopeType targetScope,
    String targetRef,
    String referenceType,
    String referenceId,
    Long createdBy,
    Instant publishedAt,
    Instant createdAt,
    long totalRecipients,
    long readCount
) {
}
