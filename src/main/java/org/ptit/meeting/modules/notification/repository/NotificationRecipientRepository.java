package org.ptit.meeting.modules.notification.repository;

import java.time.Instant;
import java.util.Optional;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.UserNotificationRow;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, String> {

  Optional<NotificationRecipient> findByIdAndUserId(String id, Long userId);

  @Query(value = NotificationQueries.FIND_USER_INBOX, countQuery = NotificationQueries.COUNT_USER_INBOX)
  Page<UserNotificationRow> findUserInbox(
      @Param("userId") Long userId,
      @Param("type") NotificationType type,
      @Param("category") NotificationCategory category,
      @Param("read") Boolean read,
      Pageable pageable
  );

  @Query(NotificationQueries.COUNT_USER_UNREAD)
  long countUnreadByUserId(@Param("userId") Long userId);

  @Modifying
  @Query(NotificationQueries.MARK_ALL_READ)
  int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
