package org.ptit.meeting.modules.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
    name = "notification_recipients",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_notification_recipients_noti_user",
            columnNames = {"notification_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_noti_recipients_user_read_created", columnList = "user_id, is_read, created_at"),
        @Index(name = "idx_noti_recipients_notification_id", columnList = "notification_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecipient extends BaseEntity {

  @Column(name = "notification_id", nullable = false, length = 36)
  private String notificationId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "is_read", nullable = false)
  @Builder.Default
  private boolean read = false;

  @Column(name = "read_at")
  private Instant readAt;
}
