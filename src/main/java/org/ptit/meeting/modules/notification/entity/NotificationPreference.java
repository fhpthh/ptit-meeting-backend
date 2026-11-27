package org.ptit.meeting.modules.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;

@Entity
@Table(
    name = "notification_preferences",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_preferences_user_id", columnNames = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "in_app_enabled", nullable = false)
  @Builder.Default
  private boolean inAppEnabled = true;

  @Column(name = "zalo_enabled", nullable = false)
  @Builder.Default
  private boolean zaloEnabled = true;

  @Column(name = "reminder_minutes_before", nullable = false)
  private int reminderMinutesBefore;

  @Column(name = "receive_pgv_via_zalo", nullable = false)
  @Builder.Default
  private boolean receivePgvViaZalo = true;

  @Column(name = "receive_lecturer_via_zalo", nullable = false)
  @Builder.Default
  private boolean receiveLecturerViaZalo = true;
}
