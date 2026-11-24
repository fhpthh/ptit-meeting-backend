package org.ptit.meeting.modules.ai.entity;

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
    name = "recording_job_outbox",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_recording_outbox_dispatch",
            columnNames = {"job_id", "dispatch_version"}),
    indexes =
        @Index(name = "idx_recording_outbox_pending", columnList = "published_at,available_at"))
@Getter
@NoArgsConstructor
public class RecordingJobOutbox extends BaseEntity {
  @Column(name = "job_id", nullable = false, length = 36)
  private String jobId;

  @Column(name = "dispatch_version", nullable = false)
  private long dispatchVersion;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  public RecordingJobOutbox(String jobId, long dispatchVersion, Instant availableAt) {
    this.jobId = jobId;
    this.dispatchVersion = dispatchVersion;
    this.availableAt = availableAt;
  }
}
