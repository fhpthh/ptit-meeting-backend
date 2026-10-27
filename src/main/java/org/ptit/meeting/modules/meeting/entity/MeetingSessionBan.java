package org.ptit.meeting.modules.meeting.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ptit.meeting.common.base.BaseEntity;

@Entity
@Table(
    name = "meeting_session_bans",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_meeting_session_bans_session_user",
            columnNames = {"runtime_session_id", "user_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_meeting_session_bans_session_active",
            columnList = "runtime_session_id,revoked_at"
        ),
        @Index(name = "idx_meeting_session_bans_user", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MeetingSessionBan extends BaseEntity {

  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "banned_by_user_id", nullable = false)
  private Long bannedByUserId;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "banned_at", nullable = false)
  private Instant bannedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_by_user_id")
  private Long revokedByUserId;

  public MeetingSessionBan(
      String runtimeSessionId,
      Long userId,
      Long bannedByUserId,
      String reason
  ) {
    this.runtimeSessionId = runtimeSessionId;
    this.userId = userId;
    applyBan(bannedByUserId, reason);
  }

  public boolean isActive() {
    return revokedAt == null;
  }

  public void applyBan(Long actorUserId, String banReason) {
    bannedByUserId = actorUserId;
    reason = banReason;
    bannedAt = Instant.now();
    revokedAt = null;
    revokedByUserId = null;
  }

  public void revoke(Long actorUserId) {
    revokedAt = Instant.now();
    revokedByUserId = actorUserId;
  }
}
