package org.ptit.meeting.modules.meeting.entity;

import static lombok.AccessLevel.PROTECTED;

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
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRecordingStatus;

@Entity
@Table(
    name = "meeting_recordings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_meeting_recordings_active_slot",
        columnNames = "active_slot"
    ),
    indexes = {
        @Index(
            name = "idx_meeting_recordings_session_created",
            columnList = "runtime_session_id,created_at"
        ),
        @Index(
            name = "idx_meeting_recordings_status_activity",
            columnList = "status,last_activity_at"
        )
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MeetingRecording extends BaseEntity {

  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;

  @Column(name = "meeting_room_id", nullable = false, length = 36)
  private String meetingRoomId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private MeetingRecordingStatus status;

  @Column(name = "active_slot", length = 36)
  private String activeSlot;

  @Column(name = "object_key", nullable = false, length = 500)
  private String objectKey;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "storage_upload_id", length = 500)
  private String storageUploadId;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "requested_by_user_id", nullable = false)
  private Long requestedByUserId;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "failure_detail", length = 500)
  private String failureDetail;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long version;

  public MeetingRecording(
      String runtimeSessionId,
      String meetingRoomId,
      String objectKey,
      String mimeType,
      String storageUploadId,
      Long requestedByUserId
  ) {
    Instant now = Instant.now();
    this.runtimeSessionId = runtimeSessionId;
    this.meetingRoomId = meetingRoomId;
    this.status = MeetingRecordingStatus.ACTIVE;
    this.activeSlot = runtimeSessionId;
    this.objectKey = objectKey;
    this.mimeType = mimeType;
    this.storageUploadId = storageUploadId;
    this.requestedByUserId = requestedByUserId;
    this.startedAt = now;
    this.lastActivityAt = now;
  }

  public void touchUpload() {
    if (status == MeetingRecordingStatus.ACTIVE) {
      lastActivityAt = Instant.now();
    }
  }

  public void beginFinalizing() {
    if (status == MeetingRecordingStatus.ACTIVE) {
      status = MeetingRecordingStatus.FINALIZING;
      lastActivityAt = Instant.now();
    }
  }

  public void resumeUpload() {
    if (status == MeetingRecordingStatus.FINALIZING) {
      status = MeetingRecordingStatus.ACTIVE;
      lastActivityAt = Instant.now();
    }
  }

  public void complete(long completedSizeBytes, long completedDurationMs) {
    status = MeetingRecordingStatus.COMPLETED;
    sizeBytes = completedSizeBytes;
    durationMs = completedDurationMs;
    finish(null);
  }

  public void abort(String detail) {
    status = MeetingRecordingStatus.ABORTED;
    finish(detail);
  }

  public void fail(String detail) {
    status = MeetingRecordingStatus.FAILED;
    finish(detail);
  }

  private void finish(String detail) {
    Instant now = Instant.now();
    activeSlot = null;
    storageUploadId = null;
    endedAt = now;
    lastActivityAt = now;
    failureDetail = normalizeFailureDetail(detail);
  }

  private String normalizeFailureDetail(String detail) {
    if (detail == null || detail.isBlank()) {
      return null;
    }
    String normalized = detail.trim();
    return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
  }
}
