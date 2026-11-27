package org.ptit.meeting.modules.notification.dto.internal;

import java.util.Set;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;

public record NotificationPublishCommand(
    NotificationType type,
    NotificationCategory category,
    NotificationPriority priority,
    String title,
    String content,
    TargetScopeType targetScope,
    String targetRef,
    String referenceType,
    String referenceId,
    String dedupeKey,
    Long createdBy,
    Set<Long> recipientUserIds,
    Set<DeliveryChannel> channels
) {
}
