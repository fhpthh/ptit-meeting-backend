package org.ptit.meeting.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus;
import org.ptit.meeting.modules.ai.repository.AiRealtimeSessionRepository;
import org.ptit.meeting.modules.ai.service.AiWorkerAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiWorkerAuthorizationServiceImpl implements AiWorkerAuthorizationService {

  private static final String WORKER_KIND = "ai-caption-worker";

  private final AiRealtimeSessionRepository repository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public boolean isAuthorized(
      String runtimeSessionId,
      LiveKitParticipantSnapshot participant
  ) {
    if (!expectedIdentity(runtimeSessionId).equals(participant.identity())) return false;
    try {
      JsonNode metadata = objectMapper.readTree(participant.metadata());
      if (metadata.path("schemaVersion").asInt() != 1
          || !WORKER_KIND.equals(metadata.path("kind").asText())
          || !runtimeSessionId.equals(metadata.path("runtimeSessionId").asText())) {
        return false;
      }
      long epoch = metadata.path("enableEpoch").asLong(-1);
      return repository.findByRuntimeSessionId(runtimeSessionId)
          .filter(session -> session.getEnableEpoch() == epoch)
          .filter(session -> session.getStatus() == AiRealtimeStatus.STARTING
              || session.getStatus() == AiRealtimeStatus.ACTIVE)
          .isPresent();
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      return false;
    }
  }

  private String expectedIdentity(String runtimeSessionId) {
    return "ai-worker-" + runtimeSessionId;
  }
}
