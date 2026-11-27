package org.ptit.meeting.modules.notification.mapper;

import org.ptit.meeting.modules.notification.dto.internal.NotificationStatsRow;
import org.ptit.meeting.modules.notification.dto.internal.UserNotificationRow;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.dto.response.NotificationPreferenceResponse;
import org.ptit.meeting.modules.notification.dto.response.UserNotificationResponse;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public AnnouncementResponse toAnnouncementResponse(NotificationStatsRow row) {
    return new AnnouncementResponse(
        row.id(),
        row.type(),
        row.category(),
        row.priority(),
        row.title(),
        row.content(),
        row.targetScope(),
        row.targetRef(),
        row.referenceType(),
        row.referenceId(),
        row.createdBy(),
        row.publishedAt(),
        row.createdAt(),
        row.totalRecipients(),
        row.readCount()
    );
  }

  public AnnouncementResponse toAnnouncementResponse(
      Notification notification,
      long totalRecipients,
      long readCount
  ) {
    return new AnnouncementResponse(
        notification.getId(),
        notification.getType(),
        notification.getCategory(),
        notification.getPriority(),
        notification.getTitle(),
        notification.getContent(),
        notification.getTargetScope(),
        notification.getTargetRef(),
        notification.getReferenceType(),
        notification.getReferenceId(),
        notification.getCreatedBy(),
        notification.getPublishedAt(),
        notification.getCreatedAt(),
        totalRecipients,
        readCount
    );
  }

  public UserNotificationResponse toUserNotificationResponse(UserNotificationRow row) {
    return new UserNotificationResponse(
        row.recipientId(),
        row.notificationId(),
        row.type(),
        row.category(),
        row.priority(),
        row.title(),
        row.content(),
        row.referenceType(),
        row.referenceId(),
        row.createdBy(),
        row.read(),
        row.readAt(),
        row.publishedAt()
    );
  }

  public NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
    return new NotificationPreferenceResponse(
        preference.isInAppEnabled(),
        preference.isZaloEnabled(),
        preference.getReminderMinutesBefore(),
        preference.isReceivePgvViaZalo(),
        preference.isReceiveLecturerViaZalo()
    );
  }
}
