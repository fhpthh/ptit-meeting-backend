package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.internal.ParticipantConnectionEvent;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.LiveKitWebhookEventService;
import org.ptit.meeting.modules.meeting.service.MeetingAttendanceService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.ptit.meeting.modules.meeting.service.MeetingWebhookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingWebhookServiceImpl implements MeetingWebhookService {

  private static final String PARTICIPANT_JOINED = "participant_joined";
  private static final String PARTICIPANT_LEFT = "participant_left";
  private static final String PARTICIPANT_CONNECTION_ABORTED =
      "participant_connection_aborted";

  private final LiveKitWebhookEventService webhookEventService;
  private final MeetingAttendanceService attendanceService;
  private final MeetingModerationService moderationService;
  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void process(LiveKitWebhookPayload payload) {
    if (!claim(payload)) {
      return;
    }

    try {
      ProcessingOutcome outcome = dispatch(payload);
      if (outcome.processed()) {
        webhookEventService.markProcessed(payload.eventId());
      } else {
        webhookEventService.markIgnored(payload.eventId(), outcome.reason());
      }
    } catch (RuntimeException exception) {
      markFailedSafely(payload.eventId(), exception);
      throw exception;
    }
  }

  private boolean claim(LiveKitWebhookPayload payload) {
    try {
      return webhookEventService.claim(payload);
    } catch (DataIntegrityViolationException exception) {
      log.debug("Duplicate LiveKit webhook received: eventId={}", payload.eventId());
      return webhookEventService.claim(payload);
    }
  }

  private ProcessingOutcome dispatch(LiveKitWebhookPayload payload) {
    if (!isParticipantConnectionEvent(payload.eventType())) {
      return ProcessingOutcome.ignored("UNSUPPORTED_EVENT_TYPE");
    }
    if (!hasParticipantData(payload)) {
      return ProcessingOutcome.ignored("PARTICIPANT_DATA_MISSING");
    }

    Optional<MeetingRuntimeSession> runtimeSession = runtimeSessionRepository
        .findByProviderRoomName(payload.roomName());
    if (runtimeSession.isEmpty()) {
      return ProcessingOutcome.ignored("RUNTIME_SESSION_NOT_FOUND");
    }

    Optional<ParticipantMetadata> metadata = parseMetadata(payload);
    if (metadata.isEmpty()
        || !isConsistent(payload, runtimeSession.get(), metadata.get())) {
      return ProcessingOutcome.ignored("PARTICIPANT_METADATA_INVALID");
    }
    if (PARTICIPANT_JOINED.equals(payload.eventType())
        && moderationService.enforceBan(
            runtimeSession.get().getId(),
            metadata.get().userId()
        )) {
      return ProcessingOutcome.ignored("PARTICIPANT_BANNED");
    }
    if (PARTICIPANT_JOINED.equals(payload.eventType())
        && runtimeSession.get().getStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      return ProcessingOutcome.ignored("RUNTIME_SESSION_NOT_ACTIVE");
    }

    ParticipantConnectionEvent connectionEvent = new ParticipantConnectionEvent(
        runtimeSession.get().getId(),
        metadata.get().userId(),
        payload.participantIdentity(),
        payload.participantSid(),
        payload.createdAt(),
        payload.disconnectReason()
    );
    processConnectionEvent(payload.eventType(), connectionEvent);
    return ProcessingOutcome.processedSuccessfully();
  }

  private void processConnectionEvent(
      String eventType,
      ParticipantConnectionEvent connectionEvent
  ) {
    switch (eventType) {
      case PARTICIPANT_JOINED -> attendanceService.recordJoined(connectionEvent);
      case PARTICIPANT_LEFT -> attendanceService.recordLeft(connectionEvent);
      case PARTICIPANT_CONNECTION_ABORTED -> attendanceService.recordAborted(connectionEvent);
      default -> throw new IllegalStateException("Unsupported participant event type");
    }
  }

  private boolean isParticipantConnectionEvent(String eventType) {
    return PARTICIPANT_JOINED.equals(eventType)
        || PARTICIPANT_LEFT.equals(eventType)
        || PARTICIPANT_CONNECTION_ABORTED.equals(eventType);
  }

  private boolean hasParticipantData(LiveKitWebhookPayload payload) {
    return hasText(payload.roomName())
        && hasText(payload.participantSid())
        && hasText(payload.participantIdentity())
        && hasText(payload.participantMetadata());
  }

  private Optional<ParticipantMetadata> parseMetadata(LiveKitWebhookPayload payload) {
    try {
      JsonNode metadata = objectMapper.readTree(payload.participantMetadata());
      JsonNode userIdNode = metadata.path("userId");
      JsonNode runtimeSessionIdNode = metadata.path("runtimeSessionId");
      if (!userIdNode.canConvertToLong() || !runtimeSessionIdNode.isTextual()) {
        return Optional.empty();
      }

      long userId = userIdNode.longValue();
      String runtimeSessionId = runtimeSessionIdNode.textValue();
      if (userId <= 0 || !hasText(runtimeSessionId)) {
        return Optional.empty();
      }
      return Optional.of(new ParticipantMetadata(userId, runtimeSessionId));
    } catch (JsonProcessingException | RuntimeException exception) {
      return Optional.empty();
    }
  }

  private boolean isConsistent(
      LiveKitWebhookPayload payload,
      MeetingRuntimeSession runtimeSession,
      ParticipantMetadata metadata
  ) {
    return runtimeSession.getId().equals(metadata.runtimeSessionId())
        && payload.participantIdentity().equals("user-" + metadata.userId());
  }

  private void markFailedSafely(String eventId, RuntimeException exception) {
    try {
      webhookEventService.markFailed(eventId, exception.getClass().getSimpleName());
    } catch (RuntimeException persistenceException) {
      log.error("Unable to mark LiveKit webhook as failed: eventId={}", eventId);
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record ParticipantMetadata(Long userId, String runtimeSessionId) {
  }

  private record ProcessingOutcome(boolean processed, String reason) {

    private static ProcessingOutcome processedSuccessfully() {
      return new ProcessingOutcome(true, null);
    }

    private static ProcessingOutcome ignored(String reason) {
      return new ProcessingOutcome(false, reason);
    }
  }
}
