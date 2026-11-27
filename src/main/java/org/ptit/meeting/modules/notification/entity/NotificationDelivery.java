package org.ptit.meeting.modules.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryStatus;

@Entity
@Table(
    name = "notification_deliveries",
    indexes = {
        @Index(name = "idx_noti_deliveries_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_noti_deliveries_notification_channel", columnList = "notification_id, channel")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDelivery extends BaseEntity {

  @Column(name = "notification_id", nullable = false, length = 36)
  private String notificationId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private DeliveryChannel channel;

  @Column(name = "destination", length = 150)
  private String destination;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private DeliveryStatus status = DeliveryStatus.PENDING;

  @Column(name = "attempt_count", nullable = false)
  @Builder.Default
  private int attemptCount = 0;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "provider_message_id", length = 100)
  private String providerMessageId;

  @Column(name = "error_code", length = 50)
  private String errorCode;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "sent_at")
  private Instant sentAt;
}
