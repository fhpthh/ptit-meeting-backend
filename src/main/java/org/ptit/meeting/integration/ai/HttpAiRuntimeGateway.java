package org.ptit.meeting.integration.ai;

import org.ptit.meeting.integration.ai.dto.AiRuntimeStartCommand;
import org.ptit.meeting.integration.ai.dto.AiRuntimeView;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

final class HttpAiRuntimeGateway implements AiRuntimeGateway {

  private final RestClient client;

  HttpAiRuntimeGateway(RestClient client) {
    this.client = client;
  }

  @Override
  public AiRuntimeView start(AiRuntimeStartCommand command) {
    return execute(() -> client.post().uri("/internal/v1/realtime-sessions/start")
        .body(command).retrieve().body(AiRuntimeView.class));
  }

  @Override
  public AiRuntimeView stop(String runtimeSessionId) {
    return execute(() -> client.post()
        .uri("/internal/v1/realtime-sessions/{id}/stop", runtimeSessionId)
        .retrieve().body(AiRuntimeView.class));
  }

  @Override
  public AiRuntimeView status(String runtimeSessionId) {
    return execute(() -> client.get()
        .uri("/internal/v1/realtime-sessions/{id}", runtimeSessionId)
        .retrieve().body(AiRuntimeView.class));
  }

  private AiRuntimeView execute(Request request) {
    try {
      AiRuntimeView response = request.call();
      if (response == null) throw new RestClientException("AI runtime returned no body");
      return response;
    } catch (RestClientResponseException exception) {
      throw new AiRuntimeIntegrationException(
          "AI runtime request failed", exception, exception.getStatusCode().value());
    } catch (RestClientException exception) {
      throw new AiRuntimeIntegrationException("AI runtime request failed", exception);
    }
  }

  @FunctionalInterface
  private interface Request {
    AiRuntimeView call();
  }
}
