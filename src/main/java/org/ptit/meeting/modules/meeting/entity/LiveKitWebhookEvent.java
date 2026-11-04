package org.ptit.meeting.modules.meeting.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.WebhookProcessingStatus;

@Entity
@Table(
    name = "livekit_webhook_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_livekit_webhook_event_provider_id",
            columnNames = "provider_event_id"
        )
    },
    indexes = {
        @Index(
            name = "idx_livekit_webhook_events_status_created",
            columnList = "processing_status,created_at"
        )
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class LiveKitWebhookEvent extends BaseEntity {

  @Column(name = "provider_event_id", nullable = false, length = 100)
  private String providerEventId;

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  @Column(name = "provider_created_at", nullable = false)
  private Instant providerCreatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 30)
  private WebhookProcessingStatus processingStatus;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  public LiveKitWebhookEvent(
      String providerEventId,
      String eventType,
      Instant providerCreatedAt
  ) {
    this.providerEventId = providerEventId;
    this.eventType = eventType;
    this.providerCreatedAt = providerCreatedAt;
    this.processingStatus = WebhookProcessingStatus.PROCESSING;
  }

  public boolean isFinished() {
    return processingStatus == WebhookProcessingStatus.PROCESSED
        || processingStatus == WebhookProcessingStatus.IGNORED;
  }

  public void retry() {
    processingStatus = WebhookProcessingStatus.PROCESSING;
    processedAt = null;
    failureReason = null;
  }

  public void markProcessed() {
    processingStatus = WebhookProcessingStatus.PROCESSED;
    processedAt = Instant.now();
    failureReason = null;
  }

  public void markIgnored(String reason) {
    processingStatus = WebhookProcessingStatus.IGNORED;
    processedAt = Instant.now();
    failureReason = reason;
  }

  public void markFailed(String reason) {
    processingStatus = WebhookProcessingStatus.FAILED;
    processedAt = Instant.now();
    failureReason = reason;
  }
}
