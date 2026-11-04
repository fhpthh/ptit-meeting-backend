package org.ptit.meeting.modules.meeting.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.ParticipantConnectionStatus;

@Entity
@Table(
    name = "meeting_participant_connections",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_meeting_connections_session_sid",
            columnNames = {"runtime_session_id", "participant_sid"}
        )
    },
    indexes = {
        @Index(
            name = "idx_meeting_connections_session_user",
            columnList = "runtime_session_id,user_id"
        ),
        @Index(
            name = "idx_meeting_connections_session_identity",
            columnList = "runtime_session_id,participant_identity"
        ),
        @Index(
            name = "idx_meeting_connections_open",
            columnList = "runtime_session_id,left_at"
        )
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ParticipantConnection extends BaseEntity {

  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "participant_identity", nullable = false, length = 128)
  private String participantIdentity;

  @Column(name = "participant_sid", nullable = false, length = 128)
  private String participantSid;

  @Column(name = "joined_at")
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  @Column(name = "duration_seconds")
  private Long durationSeconds;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ParticipantConnectionStatus status;

  @Column(name = "disconnect_reason", length = 100)
  private String disconnectReason;

  public ParticipantConnection(
      String runtimeSessionId,
      Long userId,
      String participantIdentity,
      String participantSid
  ) {
    this.runtimeSessionId = runtimeSessionId;
    this.userId = userId;
    this.participantIdentity = participantIdentity;
    this.participantSid = participantSid;
  }

  public void markJoined(Instant eventTime) {
    if (joinedAt == null || eventTime.isBefore(joinedAt)) {
      joinedAt = eventTime;
    }
    if (leftAt == null) {
      status = ParticipantConnectionStatus.ACTIVE;
      durationSeconds = null;
      disconnectReason = null;
      return;
    }
    updateClosedDuration();
  }

  public void markLeft(Instant eventTime, String reason) {
    close(eventTime, reason, ParticipantConnectionStatus.LEFT);
  }

  public void markAborted(Instant eventTime, String reason) {
    close(eventTime, reason, ParticipantConnectionStatus.ABORTED);
  }

  private void close(
      Instant eventTime,
      String reason,
      ParticipantConnectionStatus closedStatus
  ) {
    if (leftAt == null || eventTime.isAfter(leftAt)) {
      leftAt = eventTime;
    }
    status = closedStatus;
    disconnectReason = reason;
    updateClosedDuration();
  }

  private void updateClosedDuration() {
    if (joinedAt == null || leftAt == null || leftAt.isBefore(joinedAt)) {
      durationSeconds = 0L;
      return;
    }
    durationSeconds = Duration.between(joinedAt, leftAt).getSeconds();
  }
}
