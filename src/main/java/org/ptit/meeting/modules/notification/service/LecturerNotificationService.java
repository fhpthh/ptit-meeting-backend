package org.ptit.meeting.modules.notification.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.dto.request.LecturerAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;

public interface LecturerNotificationService {

  AnnouncementResponse createAnnouncement(
      Long lecturerUserId,
      LecturerAnnouncementCreateRequest request
  );

  PageResponseDTO<AnnouncementResponse> listOwnAnnouncements(
      Long lecturerUserId,
      NotificationCategory category,
      NotificationPriority priority,
      String keyword,
      int page,
      int size
  );

  AnnouncementResponse getOwnAnnouncementDetail(Long lecturerUserId, String notificationId);

  void deleteOwnAnnouncement(Long lecturerUserId, String notificationId);
}
