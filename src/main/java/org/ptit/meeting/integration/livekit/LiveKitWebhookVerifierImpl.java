package org.ptit.meeting.integration.livekit;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.livekit.server.WebhookReceiver;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import livekit.LivekitModels.ParticipantInfo;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.LiveKitProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;

@Slf4j
final class LiveKitWebhookVerifierImpl implements LiveKitWebhookVerifier {

  private static final String BEARER_PREFIX = "Bearer ";

  private final WebhookReceiver webhookReceiver;
  private final LiveKitProperties properties;

  LiveKitWebhookVerifierImpl(
      WebhookReceiver webhookReceiver,
      LiveKitProperties properties
  ) {
    this.webhookReceiver = webhookReceiver;
    this.properties = properties;
  }

  @Override
  public LiveKitWebhookPayload verify(String rawBody, String authorizationHeader) {
    if (rawBody == null || rawBody.isBlank()) {
      throw BusinessException.invalidInput(MeetingErrorConstants.WEBHOOK_PAYLOAD_INVALID);
    }

    WebhookIdentity identity = identify(rawBody);
    try {
      verifyAuthorization(rawBody, authorizationHeader);
    } catch (WebhookAuthenticationException exception) {
      log.warn(
          "Rejected LiveKit webhook authentication: eventType={}, eventId={}, "
              + "bodyBytes={}, authorization={}, reason={}",
          identity.eventType(),
          identity.eventId(),
          rawBody.getBytes(StandardCharsets.UTF_8).length,
          describeAuthorization(authorizationHeader),
          exception.getMessage()
      );
      throw BusinessException.unauthorized(MeetingErrorConstants.WEBHOOK_SIGNATURE_INVALID);
    }

    try {
      WebhookEvent event = webhookReceiver.receive(rawBody, null, true);
      validateEvent(event);
      return toPayload(event);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.error(
          "Unable to parse verified LiveKit webhook: eventType={}, eventId={}, "
              + "bodyBytes={}, errorType={}, message={}",
          identity.eventType(),
          identity.eventId(),
          rawBody.getBytes(StandardCharsets.UTF_8).length,
          exception.getClass().getSimpleName(),
          exception.getMessage(),
          exception
      );
      throw BusinessException.invalidInput(MeetingErrorConstants.WEBHOOK_PAYLOAD_INVALID);
    }
  }

  private void verifyAuthorization(String rawBody, String authorizationHeader) {
    String token = extractToken(authorizationHeader);
    Claims claims;
    try {
      claims = Jwts.parser()
          .verifyWith(Keys.hmacShaKeyFor(
              properties.getApiSecret().getBytes(StandardCharsets.UTF_8)
          ))
          .requireIssuer(properties.getApiKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException exception) {
      throw new WebhookAuthenticationException("JWT_EXPIRED");
    } catch (JwtException | IllegalArgumentException exception) {
      throw new WebhookAuthenticationException("JWT_INVALID");
    }

    if (claims.getExpiration() == null) {
      throw new WebhookAuthenticationException("JWT_EXPIRATION_MISSING");
    }
    String receivedHash = claims.get("sha256", String.class);
    if (receivedHash == null || receivedHash.isBlank()) {
      throw new WebhookAuthenticationException("BODY_HASH_MISSING");
    }
    String expectedHash = bodyHash(rawBody);
    if (!MessageDigest.isEqual(
        expectedHash.getBytes(StandardCharsets.US_ASCII),
        receivedHash.getBytes(StandardCharsets.US_ASCII)
    )) {
      throw new WebhookAuthenticationException("BODY_HASH_MISMATCH");
    }
  }

  private String extractToken(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new WebhookAuthenticationException("AUTHORIZATION_MISSING");
    }
    String value = authorizationHeader.trim();
    if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      value = value.substring(BEARER_PREFIX.length()).trim();
    }
    if (value.isBlank()) {
      throw new WebhookAuthenticationException("JWT_MISSING");
    }
    return value;
  }

  private String bodyHash(String rawBody) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawBody.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private WebhookIdentity identify(String rawBody) {
    return new WebhookIdentity(
        extractJsonString(rawBody, "event"),
        extractJsonString(rawBody, "id")
    );
  }

  private String extractJsonString(String rawBody, String property) {
    String marker = "\"" + property + "\":\"";
    int start = rawBody.indexOf(marker);
    if (start < 0) {
      return "unknown";
    }
    int valueStart = start + marker.length();
    int valueEnd = rawBody.indexOf('"', valueStart);
    return valueEnd > valueStart ? rawBody.substring(valueStart, valueEnd) : "unknown";
  }

  private String describeAuthorization(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return "MISSING";
    }
    return authorizationHeader.trim().regionMatches(
        true,
        0,
        BEARER_PREFIX,
        0,
        BEARER_PREFIX.length()
    ) ? "BEARER_JWT" : "RAW_JWT";
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

  private record WebhookIdentity(String eventType, String eventId) {
  }

  private static final class WebhookAuthenticationException extends RuntimeException {

    private WebhookAuthenticationException(String reason) {
      super(reason, null, false, false);
    }
  }
}
