package org.ptit.meeting.modules.notification.repository;

import java.util.Optional;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationStatsRow;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, String> {

  Optional<Notification> findByIdAndDeletedFalse(String id);

  Optional<Notification> findByDedupeKey(String dedupeKey);

  boolean existsByDedupeKey(String dedupeKey);

  @Query(value = NotificationQueries.SEARCH_SENT_WITH_STATS, countQuery = NotificationQueries.COUNT_SENT)
  Page<NotificationStatsRow> searchSentWithStats(
      @Param("createdBy") Long createdBy,
      @Param("type") NotificationType type,
      @Param("category") NotificationCategory category,
      @Param("priority") NotificationPriority priority,
      @Param("keyword") String keyword,
      Pageable pageable
  );

  @Query(NotificationQueries.FIND_STATS_BY_ID)
  Optional<NotificationStatsRow> findStatsById(@Param("id") String id);
}
