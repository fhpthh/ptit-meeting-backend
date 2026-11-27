package org.ptit.meeting.modules.notification.repository;

public final class NotificationQueries {

  public static final String SEARCH_SENT_WITH_STATS = """
      SELECT new org.ptit.meeting.modules.notification.dto.internal.NotificationStatsRow(
          n.id, n.type, n.category, n.priority, n.title, n.content,
          n.targetScope, n.targetRef, n.referenceType, n.referenceId,
          n.createdBy, n.publishedAt, n.createdAt,
          COUNT(r.id),
          COALESCE(SUM(CASE WHEN r.read = true THEN 1L ELSE 0L END), 0L)
      )
      FROM Notification n
      LEFT JOIN NotificationRecipient r ON r.notificationId = n.id
      WHERE n.deleted = false
        AND (:createdBy IS NULL OR n.createdBy = :createdBy)
        AND (:type IS NULL OR n.type = :type)
        AND (:category IS NULL OR n.category = :category)
        AND (:priority IS NULL OR n.priority = :priority)
        AND (:keyword IS NULL
          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
      GROUP BY n.id, n.type, n.category, n.priority, n.title, n.content,
          n.targetScope, n.targetRef, n.referenceType, n.referenceId,
          n.createdBy, n.publishedAt, n.createdAt
      ORDER BY n.publishedAt DESC, n.createdAt DESC
      """;

  public static final String COUNT_SENT = """
      SELECT COUNT(n.id)
      FROM Notification n
      WHERE n.deleted = false
        AND (:createdBy IS NULL OR n.createdBy = :createdBy)
        AND (:type IS NULL OR n.type = :type)
        AND (:category IS NULL OR n.category = :category)
        AND (:priority IS NULL OR n.priority = :priority)
        AND (:keyword IS NULL
          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
      """;

  public static final String FIND_STATS_BY_ID = """
      SELECT new org.ptit.meeting.modules.notification.dto.internal.NotificationStatsRow(
          n.id, n.type, n.category, n.priority, n.title, n.content,
          n.targetScope, n.targetRef, n.referenceType, n.referenceId,
          n.createdBy, n.publishedAt, n.createdAt,
          COUNT(r.id),
          COALESCE(SUM(CASE WHEN r.read = true THEN 1L ELSE 0L END), 0L)
      )
      FROM Notification n
      LEFT JOIN NotificationRecipient r ON r.notificationId = n.id
      WHERE n.id = :id AND n.deleted = false
      GROUP BY n.id, n.type, n.category, n.priority, n.title, n.content,
          n.targetScope, n.targetRef, n.referenceType, n.referenceId,
          n.createdBy, n.publishedAt, n.createdAt
      """;

  public static final String FIND_USER_INBOX = """
      SELECT new org.ptit.meeting.modules.notification.dto.internal.UserNotificationRow(
          r.id, n.id, n.type, n.category, n.priority, n.title, n.content,
          n.referenceType, n.referenceId, n.createdBy, r.read, r.readAt, n.publishedAt
      )
      FROM NotificationRecipient r
      JOIN Notification n ON n.id = r.notificationId
      WHERE r.userId = :userId
        AND n.deleted = false
        AND (:type IS NULL OR n.type = :type)
        AND (:category IS NULL OR n.category = :category)
        AND (:read IS NULL OR r.read = :read)
      ORDER BY n.publishedAt DESC, r.createdAt DESC
      """;

  public static final String COUNT_USER_INBOX = """
      SELECT COUNT(r.id)
      FROM NotificationRecipient r
      JOIN Notification n ON n.id = r.notificationId
      WHERE r.userId = :userId
        AND n.deleted = false
        AND (:type IS NULL OR n.type = :type)
        AND (:category IS NULL OR n.category = :category)
        AND (:read IS NULL OR r.read = :read)
      """;

  public static final String COUNT_USER_UNREAD = """
      SELECT COUNT(r.id)
      FROM NotificationRecipient r
      JOIN Notification n ON n.id = r.notificationId
      WHERE r.userId = :userId
        AND r.read = false
        AND n.deleted = false
      """;

  public static final String MARK_ALL_READ = """
      UPDATE NotificationRecipient r
      SET r.read = true, r.readAt = :readAt, r.updatedAt = :readAt
      WHERE r.userId = :userId
        AND r.read = false
        AND r.notificationId IN (
          SELECT n.id FROM Notification n WHERE n.deleted = false
        )
      """;

  private NotificationQueries() {
  }
}
