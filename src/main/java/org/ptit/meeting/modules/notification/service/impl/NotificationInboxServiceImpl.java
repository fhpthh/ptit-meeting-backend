package org.ptit.meeting.modules.notification.service.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.response.MarkAllReadResponse;
import org.ptit.meeting.modules.notification.dto.response.UnreadCountResponse;
import org.ptit.meeting.modules.notification.dto.response.UserNotificationResponse;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.service.NotificationInboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationInboxServiceImpl implements NotificationInboxService {

  private final NotificationRecipientRepository recipientRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationProperties properties;

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<UserNotificationResponse> getUserInbox(
      Long userId,
      NotificationType type,
      NotificationCategory category,
      Boolean read,
      int page,
      int size
  ) {
    int safePage = Math.max(0, page);
    int requestedSize = size <= 0 ? properties.getDefaultPageSize() : size;
    int safeSize = Math.min(requestedSize, properties.getMaxPageSize());
    Pageable pageable = PageRequest.of(safePage, safeSize);

    Page<UserNotificationResponse> mappedPage = recipientRepository
        .findUserInbox(userId, type, category, read, pageable)
        .map(notificationMapper::toUserNotificationResponse);
    return PageResponseDTO.from(mappedPage);
  }

  @Override
  @Transactional(readOnly = true)
  public UnreadCountResponse getUnreadCount(Long userId) {
    return new UnreadCountResponse(recipientRepository.countUnreadByUserId(userId));
  }

  @Override
  @Transactional
  public void markAsRead(Long userId, String recipientId) {
    NotificationRecipient recipient = recipientRepository.findByIdAndUserId(recipientId, userId)
        .orElseThrow(() -> BusinessException.notExisted(
            NotificationErrorConstants.RECIPIENT_NOT_FOUND,
            recipientId
        ));
    if (!recipient.isRead()) {
      recipient.setRead(true);
      recipient.setReadAt(Instant.now());
      recipientRepository.save(recipient);
    }
  }

  @Override
  @Transactional
  public MarkAllReadResponse markAllAsRead(Long userId) {
    int updated = recipientRepository.markAllAsReadByUserId(userId, Instant.now());
    return new MarkAllReadResponse(updated);
  }
}
