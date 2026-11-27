package org.ptit.meeting.modules.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notifications_dedupe_key", columnNames = "dedupe_key")
    },
    indexes = {
        @Index(name = "idx_notifications_type_created_at", columnList = "type, created_at"),
        @Index(name = "idx_notifications_created_by_deleted", columnList = "created_by, is_deleted")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private NotificationType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 30)
  @Builder.Default
  private NotificationCategory category = NotificationCategory.GENERAL;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 20)
  @Builder.Default
  private NotificationPriority priority = NotificationPriority.NORMAL;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_scope", nullable = false, length = 30)
  private TargetScopeType targetScope;

  @Column(name = "target_ref", length = 255)
  private String targetRef;

  @Column(name = "reference_type", length = 40)
  private String referenceType;

  @Column(name = "reference_id", length = 100)
  private String referenceId;

  @Column(name = "dedupe_key", length = 150)
  private String dedupeKey;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
