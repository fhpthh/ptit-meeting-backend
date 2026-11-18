package org.ptit.meeting.modules.ai.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ptit.meeting.common.base.BaseEntity;

@Entity
@Table(name = "ai_final_captions", uniqueConstraints = @UniqueConstraint(
    name = "uk_ai_final_caption_revision",
    columnNames = {"runtime_session_id", "stream_id", "generation", "segment_id", "language", "revision"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class AiFinalCaption extends BaseEntity {
  @Column(name = "runtime_session_id", nullable = false, length = 36)
  private String runtimeSessionId;
  @Column(name = "stream_id", nullable = false, length = 256)
  private String streamId;
  @Column(name = "generation", nullable = false)
  private long generation;
  @Column(name = "segment_id", nullable = false, length = 256)
  private String segmentId;
  @Column(name = "language", nullable = false, length = 2)
  private String language;
  @Column(name = "revision", nullable = false)
  private long revision;
  @Column(name = "status", nullable = false, length = 20)
  private String status;
  @Column(name = "text", nullable = false, length = 8000)
  private String text;
  @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
  private String payloadJson;

  public AiFinalCaption(String runtimeSessionId, String streamId, long generation,
      String segmentId, String language, long revision, String status, String text,
      String payloadJson) {
    this.runtimeSessionId = runtimeSessionId;
    this.streamId = streamId;
    this.generation = generation;
    this.segmentId = segmentId;
    this.language = language;
    this.revision = revision;
    this.status = status;
    this.text = text;
    this.payloadJson = payloadJson;
  }
}
