package org.ptit.meeting.modules.ai.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus;

@Entity
@Table(name = "ai_realtime_sessions", uniqueConstraints = @UniqueConstraint(
    name = "uk_ai_realtime_runtime_session", columnNames = "runtime_session_id"))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class AiRealtimeSession extends BaseEntity {

  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;

  @Column(name = "enable_epoch", nullable = false)
  private long enableEpoch;

  @Column(name = "requested_by_user_id", nullable = false)
  private Long requestedByUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AiRealtimeStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "stopped_at")
  private Instant stoppedAt;

  @Column(name = "failure_detail", length = 500)
  private String failureDetail;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long version;

  public AiRealtimeSession(String runtimeSessionId, long enableEpoch, Long requestedByUserId) {
    this.runtimeSessionId = runtimeSessionId;
    this.enableEpoch = enableEpoch;
    this.requestedByUserId = requestedByUserId;
    this.status = AiRealtimeStatus.STARTING;
  }

  public void activate() {
    status = AiRealtimeStatus.ACTIVE;
    startedAt = Instant.now();
    stoppedAt = null;
    failureDetail = null;
  }

  public void restart(long epoch, Long userId) {
    enableEpoch = epoch;
    requestedByUserId = userId;
    status = AiRealtimeStatus.STARTING;
    stoppedAt = null;
    failureDetail = null;
  }

  public void stop() {
    status = AiRealtimeStatus.STOPPED;
    stoppedAt = Instant.now();
  }

  public void fail(String detail) {
    status = AiRealtimeStatus.FAILED;
    failureDetail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
  }
}
