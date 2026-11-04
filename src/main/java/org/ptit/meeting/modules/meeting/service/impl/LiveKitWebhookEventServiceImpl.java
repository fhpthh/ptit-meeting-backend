package org.ptit.meeting.modules.meeting.service.impl;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.constant.enums.WebhookProcessingStatus;
import org.ptit.meeting.modules.meeting.entity.LiveKitWebhookEvent;
import org.ptit.meeting.modules.meeting.repository.LiveKitWebhookEventRepository;
import org.ptit.meeting.modules.meeting.service.LiveKitWebhookEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveKitWebhookEventServiceImpl implements LiveKitWebhookEventService {

  private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);
  private static final int REASON_MAX_LENGTH = 500;

  private final LiveKitWebhookEventRepository eventRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean claim(LiveKitWebhookPayload payload) {
    LiveKitWebhookEvent existing = eventRepository.findByProviderEventId(payload.eventId())
        .orElse(null);
    if (existing == null) {
      eventRepository.saveAndFlush(new LiveKitWebhookEvent(
          payload.eventId(),
          payload.eventType(),
          payload.createdAt()
      ));
      return true;
    }
    if (existing.isFinished()) {
      return false;
    }
    if (isStillProcessing(existing)) {
      throw new IllegalStateException("Webhook event is already being processed");
    }

    existing.retry();
    eventRepository.save(existing);
    return true;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markProcessed(String eventId) {
    LiveKitWebhookEvent event = findEvent(eventId);
    event.markProcessed();
    eventRepository.save(event);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markIgnored(String eventId, String reason) {
    LiveKitWebhookEvent event = findEvent(eventId);
    event.markIgnored(normalizeReason(reason));
    eventRepository.save(event);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(String eventId, String reason) {
    LiveKitWebhookEvent event = findEvent(eventId);
    event.markFailed(normalizeReason(reason));
    eventRepository.save(event);
  }

  private boolean isStillProcessing(LiveKitWebhookEvent event) {
    if (event.getProcessingStatus() != WebhookProcessingStatus.PROCESSING) {
      return false;
    }
    return event.getUpdatedAt() != null
        && event.getUpdatedAt().isAfter(Instant.now().minus(PROCESSING_TIMEOUT));
  }

  private LiveKitWebhookEvent findEvent(String eventId) {
    return eventRepository.findByProviderEventId(eventId)
        .orElseThrow(() -> new IllegalStateException("Claimed webhook event does not exist"));
  }

  private String normalizeReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    String normalized = reason.trim();
    return normalized.length() <= REASON_MAX_LENGTH
        ? normalized
        : normalized.substring(0, REASON_MAX_LENGTH);
  }
}
