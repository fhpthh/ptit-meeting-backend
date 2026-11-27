package org.ptit.meeting.modules.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;

/** Immutable AI originals. Future human revisions must never update these columns. */
@Entity
@Table(
    name = "recording_translation_drafts",
    uniqueConstraints = @UniqueConstraint(name = "uk_recording_draft_job", columnNames = "job_id"))
@Getter
@Setter
@NoArgsConstructor
public class RecordingTranslationDraft extends BaseEntity {
  @Column(name = "job_id", nullable = false, updatable = false, length = 36)
  private String jobId;

  @Column(name = "recording_id", nullable = false, updatable = false, length = 36)
  private String recordingId;

  @Column(name = "run_id", nullable = false, updatable = false, length = 36)
  private String runId;

  @Column(name = "recording_epoch", nullable = false, updatable = false)
  private long recordingEpoch;

  @Column(name = "attempt_id", nullable = false, updatable = false, length = 36)
  private String attemptId;

  @Column(name = "fencing_token", nullable = false, updatable = false)
  private long fencingToken;

  @Column(name = "manifest_bucket", nullable = false, updatable = false, length = 63)
  private String manifestBucket;

  @Column(name = "manifest_key", nullable = false, updatable = false, length = 1024)
  private String manifestKey;

  @Column(name = "manifest_sha256", nullable = false, updatable = false, length = 64)
  private String manifestSha256;

  @Column(name = "manifest_bytes", nullable = false, updatable = false)
  private long manifestBytes;

  @Column(
      name = "manifest_json",
      nullable = false,
      updatable = false,
      columnDefinition = "LONGTEXT")
  private String manifestJson;

  @Column(
      name = "original_document_json",
      nullable = false,
      updatable = false,
      columnDefinition = "LONGTEXT")
  private String originalDocumentJson;

  @Column(name = "source_sha256", nullable = false, updatable = false, length = 64)
  private String sourceSha256;

  @Column(name = "document_revision", nullable = false, updatable = false)
  private long documentRevision;

  @Column(name = "duration_ms", nullable = false, updatable = false)
  private long durationMs;

  @Column(name = "vi_segments", nullable = false, updatable = false)
  private int viSegments;

  @Column(name = "en_segments", nullable = false, updatable = false)
  private int enSegments;
}
