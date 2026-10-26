package org.ptit.meeting.integration.livekit;

import io.livekit.server.WebhookReceiver;
import java.time.Instant;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitWebhook.WebhookEvent;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;

final class LiveKitWebhookVerifierImpl implements LiveKitWebhookVerifier {

  private final WebhookReceiver webhookReceiver;

  LiveKitWebhookVerifierImpl(WebhookReceiver webhookReceiver) {
    this.webhookReceiver = webhookReceiver;
  }

  @Override
  public LiveKitWebhookPayload verify(String rawBody, String authorizationHeader) {
    if (rawBody == null || rawBody.isBlank()) {
      throw BusinessException.invalidInput(MeetingErrorConstants.WEBHOOK_PAYLOAD_INVALID);
    }

    try {
      WebhookEvent event = webhookReceiver.receive(rawBody, authorizationHeader);
      validateEvent(event);
      return toPayload(event);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw BusinessException.unauthorized(MeetingErrorConstants.WEBHOOK_SIGNATURE_INVALID);
    }
  }

  private void validateEvent(WebhookEvent event) {
    if (event.getId().isBlank() || event.getEvent().isBlank() || event.getCreatedAt() <= 0) {
      throw BusinessException.invalidInput(MeetingErrorConstants.WEBHOOK_PAYLOAD_INVALID);
    }
  }

  private LiveKitWebhookPayload toPayload(WebhookEvent event) {
    ParticipantInfo participant = event.hasParticipant() ? event.getParticipant() : null;
    return new LiveKitWebhookPayload(
        event.getId(),
        event.getEvent(),
        Instant.ofEpochSecond(event.getCreatedAt()),
        event.hasRoom() ? emptyToNull(event.getRoom().getName()) : null,
        participant == null ? null : emptyToNull(participant.getSid()),
        participant == null ? null : emptyToNull(participant.getIdentity()),
        participant == null ? null : emptyToNull(participant.getMetadata()),
        participant == null ? null : disconnectReason(participant)
    );
  }

  private String disconnectReason(ParticipantInfo participant) {
    String reason = participant.getDisconnectReason().name();
    return "UNKNOWN_REASON".equals(reason) ? null : reason;
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
