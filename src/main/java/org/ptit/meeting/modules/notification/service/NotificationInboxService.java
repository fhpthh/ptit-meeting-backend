package org.ptit.meeting.modules.notification.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.response.MarkAllReadResponse;
import org.ptit.meeting.modules.notification.dto.response.UnreadCountResponse;
import org.ptit.meeting.modules.notification.dto.response.UserNotificationResponse;

public interface NotificationInboxService {

  PageResponseDTO<UserNotificationResponse> getUserInbox(
      Long userId,
      NotificationType type,
      NotificationCategory category,
      Boolean read,
      int page,
      int size
  );

  UnreadCountResponse getUnreadCount(Long userId);

  void markAsRead(Long userId, String recipientId);

  MarkAllReadResponse markAllAsRead(Long userId);
}
