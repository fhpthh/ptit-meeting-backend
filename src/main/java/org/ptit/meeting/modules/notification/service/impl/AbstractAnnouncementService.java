package org.ptit.meeting.modules.notification.service.impl;

import java.time.Instant;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationStatsRow;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Template base class sharing common announcement query, pagination, detail lookup,
 * and revocation flows across PGV and Lecturer services.
 */
public abstract class AbstractAnnouncementService {

  protected final NotificationRepository notificationRepository;
  protected final NotificationPublisherService publisherService;
  protected final NotificationMapper notificationMapper;
  protected final NotificationProperties properties;

  protected AbstractAnnouncementService(
      NotificationRepository notificationRepository,
      NotificationPublisherService publisherService,
      NotificationMapper notificationMapper,
      NotificationProperties properties
  ) {
    this.notificationRepository = notificationRepository;
    this.publisherService = publisherService;
    this.notificationMapper = notificationMapper;
    this.properties = properties;
  }

  protected PageResponseDTO<AnnouncementResponse> searchSentAnnouncements(
      Long createdByFilter,
      NotificationType type,
      NotificationCategory category,
      NotificationPriority priority,
      String keyword,
      int page,
      int size
  ) {
    Pageable pageable = buildPageable(page, size);
    String normalizedKeyword = normalizeKeyword(keyword);
    Page<AnnouncementResponse> mappedPage = notificationRepository
        .searchSentWithStats(createdByFilter, type, category, priority, normalizedKeyword, pageable)
        .map(notificationMapper::toAnnouncementResponse);
    return PageResponseDTO.from(mappedPage);
  }

  protected AnnouncementResponse getAnnouncementDetailWithAccessCheck(
      Long actorUserId,
      String notificationId
  ) {
    NotificationStatsRow row = notificationRepository.findStatsById(notificationId)
        .orElseThrow(() -> BusinessException.notExisted(
            NotificationErrorConstants.NOT_FOUND,
            notificationId
        ));
    verifyAccess(actorUserId, row.type(), row.createdBy(), row.id());
    return notificationMapper.toAnnouncementResponse(row);
  }

  protected void softDeleteAnnouncementWithAccessCheck(Long actorUserId, String notificationId) {
    Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
        .orElseThrow(() -> BusinessException.notExisted(
            NotificationErrorConstants.NOT_FOUND,
            notificationId
        ));
    verifyAccess(actorUserId, notification.getType(), notification.getCreatedBy(), notification.getId());
    notification.setDeleted(true);
    notification.setDeletedAt(Instant.now());
    notificationRepository.save(notification);
  }

  protected abstract void verifyAccess(
      Long actorUserId,
      NotificationType type,
      Long createdBy,
      String notificationId
  );

  protected Pageable buildPageable(int page, int size) {
    int safePage = Math.max(0, page);
    int requestedSize = size <= 0 ? properties.getDefaultPageSize() : size;
    int safeSize = Math.min(requestedSize, properties.getMaxPageSize());
    return PageRequest.of(safePage, safeSize);
  }

  protected String normalizeKeyword(String keyword) {
    return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
  }
}
