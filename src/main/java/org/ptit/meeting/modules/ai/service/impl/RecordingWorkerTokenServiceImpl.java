package org.ptit.meeting.modules.ai.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.service.RecordingWorkerTokenService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingWorkerTokenServiceImpl implements RecordingWorkerTokenService {
  private static final String ISSUER = "ptit-meeting-recording";
  private static final String AUDIENCE = "ptit-recording-jobs";
  private final AiRecordingProperties properties;

  @Override
  public String issue(RecordingWorkerIdentity identity) {
    Instant now = Instant.now();
    var builder =
        Jwts.builder()
            .issuer(ISSUER)
            .audience()
            .add(AUDIENCE)
            .and()
            .id(UUID.randomUUID().toString())
            .subject(identity.workerId())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.getTokenTtlSeconds())))
            .claim("jobId", identity.jobId())
            .claim("scope", identity.scope());
    if ("attempt".equals(identity.scope())) {
      builder
          .claim("recordingId", identity.recordingId())
          .claim("runId", identity.runId())
          .claim("recordingEpoch", identity.recordingEpoch())
          .claim("attemptId", identity.attemptId())
          .claim("fencingToken", identity.fencingToken());
    }
    return builder.signWith(key(), Jwts.SIG.HS256).compact();
  }

  @Override
  public RecordingWorkerIdentity verify(String token) {
    try {
      var signed =
          Jwts.parser()
              .verifyWith(key())
              .requireIssuer(ISSUER)
              .requireAudience(AUDIENCE)
              .build()
              .parseSignedClaims(token);
      Claims claims = signed.getPayload();
      if (!"HS256".equals(signed.getHeader().getAlgorithm())
          || claims.getExpiration() == null
          || claims.getIssuedAt() == null
          || claims.getId() == null
          || claims.getIssuedAt().toInstant().isAfter(Instant.now().plusSeconds(5))
          || claims
              .getExpiration()
              .toInstant()
              .isAfter(
                  claims.getIssuedAt().toInstant().plusSeconds(properties.getTokenTtlSeconds()))) {
        throw invalid();
      }
      String scope = claims.get("scope", String.class);
      identifier(claims.getId());
      String worker = identifier(claims.getSubject());
      String job = identifier(claims.get("jobId", String.class));
      if ("claim".equals(scope)) {
        return new RecordingWorkerIdentity(job, worker, scope, null, null, 0, null, 0);
      }
      if (!"attempt".equals(scope)) throw invalid();
      return new RecordingWorkerIdentity(
          job,
          worker,
          scope,
          identifier(claims.get("recordingId", String.class)),
          identifier(claims.get("runId", String.class)),
          positive(claims, "recordingEpoch"),
          identifier(claims.get("attemptId", String.class)),
          positive(claims, "fencingToken"));
    } catch (JwtException | IllegalArgumentException exception) {
      throw invalid();
    }
  }

  private long positive(Claims claims, String name) {
    Object value = claims.get(name);
    if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 1) {
      throw invalid();
    }
    return ((Number) value).longValue();
  }

  private String identifier(String value) {
    if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) throw invalid();
    return value;
  }

  private SecretKey key() {
    String secret = properties.getWorkerSecret();
    if (secret == null
        || secret.getBytes(StandardCharsets.UTF_8).length < 32
        || properties.getTokenTtlSeconds() < 60
        || properties.getTokenTtlSeconds() > 3600) {
      throw invalid();
    }
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  private BusinessException invalid() {
    return new BusinessException(
        StatusCode.AUTHENTICATION_FAILED, AiErrorConstants.RECORDING_WORKER_TOKEN);
  }
}
