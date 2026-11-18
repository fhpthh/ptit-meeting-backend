package org.ptit.meeting.modules.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.AiRealtimeProperties;
import org.ptit.meeting.modules.ai.dto.request.AiFinalCaptionRequest;
import org.ptit.meeting.modules.ai.entity.AiFinalCaption;
import org.ptit.meeting.modules.ai.repository.AiFinalCaptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/ai-realtime/final-captions")
@RequiredArgsConstructor
public class AiFinalCaptionInternalController {
  private final AiRealtimeProperties properties;
  private final AiFinalCaptionRepository repository;
  private final ObjectMapper objectMapper;

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void ingest(@RequestHeader("Authorization") String authorization,
      @RequestBody AiFinalCaptionRequest payload) {
    authenticate(authorization);
    require(payload.schemaVersion() != null && payload.schemaVersion() == 1, "unsupported schema");
    String status = text(payload.status(), "status");
    require(status.equals("FINAL") || status.equals("CORRECTION"), "caption is not durable");
    String sessionId = text(payload.sessionId(), "sessionId");
    String streamId = text(payload.streamId(), "streamId");
    long generation = nonNegative(payload.generation(), "generation");
    String segmentId = text(payload.segmentId(), "segmentId");
    String language = text(payload.language(), "language");
    long revision = nonNegative(payload.revision(), "revision");
    if (repository.existsByRuntimeSessionIdAndStreamIdAndGenerationAndSegmentIdAndLanguageAndRevision(
        sessionId, streamId, generation, segmentId, language, revision)) return;
    try {
      repository.save(new AiFinalCaption(sessionId, streamId, generation, segmentId, language,
          revision, status, text(payload.text(), "text"), objectMapper.writeValueAsString(payload)));
    } catch (JsonProcessingException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid caption payload", exception);
    }
  }

  private void authenticate(String authorization) {
    byte[] actual = authorization.getBytes(StandardCharsets.UTF_8);
    byte[] expected = ("Bearer " + properties.getServiceSecret()).getBytes(StandardCharsets.UTF_8);
    if (!properties.isEnabled() || !MessageDigest.isEqual(actual, expected)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private String text(String rawValue, String field) {
    String value = rawValue == null ? "" : rawValue.trim();
    require(!value.isEmpty(), field + " is required");
    return value;
  }

  private long nonNegative(Long value, String field) {
    require(value != null && value >= 0, field + " is invalid");
    return value;
  }

  private void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
