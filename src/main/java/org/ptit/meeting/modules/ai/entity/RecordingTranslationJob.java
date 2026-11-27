package org.ptit.meeting.modules.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStage;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;

@Entity
@Table(
    name = "recording_translation_jobs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_recording_job_idempotency",
          columnNames = {"recording_id", "requested_by_user_id", "idempotency_hash"}),
      @UniqueConstraint(name = "uk_recording_job_active_slot", columnNames = "active_slot")
    },
    indexes = {
      @Index(name = "idx_recording_jobs_created", columnList = "recording_id,created_at"),
      @Index(name = "idx_recording_jobs_recovery", columnList = "status,lease_until,retry_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class RecordingTranslationJob extends BaseEntity {
  @Column(name = "recording_id", nullable = false, length = 36)
  private String recordingId;

  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;

  @Column(name = "run_id", nullable = false, length = 36)
  private String runId;

  @Column(name = "recording_epoch", nullable = false)
  private long recordingEpoch = 1;

  @Column(name = "requested_by_user_id", nullable = false)
  private Long requestedByUserId;

  @Column(name = "idempotency_hash", nullable = false, length = 64)
  private String idempotencyHash;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "active_slot", length = 36)
  private String activeSlot;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24, columnDefinition = "varchar(24)")
  private RecordingTranslationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage", nullable = false, length = 16, columnDefinition = "varchar(16)")
  private RecordingTranslationStage stage;

  @Column(name = "progress_percent", nullable = false)
  private int progressPercent;

  @Column(name = "attempt", nullable = false)
  private int attempt;

  @Column(name = "max_attempts", nullable = false)
  private int maxAttempts;

  @Column(name = "attempt_id", length = 36)
  private String attemptId;

  @Column(name = "worker_id", length = 128)
  private String workerId;

  @Column(name = "claim_request_id", length = 128)
  private String claimRequestId;

  @Column(name = "fencing_token", nullable = false)
  private long fencingToken;

  @Column(name = "lease_until")
  private Instant leaseUntil;

  @Column(name = "progress_sequence", nullable = false)
  private long progressSequence;

  @Column(name = "progress_hash", length = 64)
  private String progressHash;

  @Column(name = "retry_at")
  private Instant retryAt;

  @Column(name = "error_code", length = 64)
  private String errorCode;

  @Column(name = "failure_hash", length = 64)
  private String failureHash;

  @Column(name = "source_bucket", nullable = false, length = 63)
  private String sourceBucket;

  @Column(name = "source_object_key", nullable = false, length = 1024)
  private String sourceObjectKey;

  @Column(name = "source_size_bytes", nullable = false)
  private long sourceSizeBytes;

  @Column(name = "source_sha256", length = 64)
  private String sourceSha256;

  @Column(name = "profile_id", nullable = false, length = 128)
  private String profileId;

  @Column(name = "profile_version", nullable = false)
  private int profileVersion;

  @Column(name = "settings_sha256", nullable = false, length = 64)
  private String settingsSha256;

  @Column(name = "model_manifest_sha256", nullable = false, length = 64)
  private String modelManifestSha256;

  @Column(name = "course_context_json", columnDefinition = "TEXT")
  private String courseContextJson;

  @Column(name = "checkpoint_key", length = 1024)
  private String checkpointKey;

  @Column(name = "checkpoint_sha256", length = 64)
  private String checkpointSha256;

  @Column(name = "checkpoint_sample", nullable = false)
  private long checkpointSample;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long version;
}
