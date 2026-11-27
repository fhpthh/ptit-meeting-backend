package org.ptit.meeting.modules.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;

@Entity
@Table(
    name = "recording_subtitle_reviews",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_recording_subtitle_review", columnNames = "recording_id"))
@Getter
@Setter
@NoArgsConstructor
public class RecordingSubtitleReview extends BaseEntity {
  @Column(name = "recording_id", nullable = false, length = 36)
  private String recordingId;

  @Column(name = "draft_id", nullable = false, length = 36)
  private String draftId;

  @Column(name = "document_json", nullable = false, columnDefinition = "LONGTEXT")
  private String documentJson;

  @Column(name = "published_json", columnDefinition = "LONGTEXT")
  private String publishedJson;

  @Column(name = "revision", nullable = false)
  private long revision;

  @Column(name = "published_revision")
  private Long publishedRevision;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;
}
