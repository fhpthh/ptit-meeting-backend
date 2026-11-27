package org.ptit.meeting.modules.notification.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.request.AdminAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;

public interface NotificationAdminService {

  AnnouncementResponse createAnnouncement(Long actorUserId, AdminAnnouncementCreateRequest request);

  PageResponseDTO<AnnouncementResponse> listAnnouncements(
      NotificationType type,
      NotificationCategory category,
      NotificationPriority priority,
      String keyword,
      int page,
      int size
  );

  AnnouncementResponse getAnnouncementDetail(String notificationId);

  void deleteAnnouncement(String notificationId);
}
