package org.ptit.meeting.integration.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.ptit.meeting.config.AiRealtimeProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RecordingTextTranslationGateway {
  private final AiRealtimeProperties properties;

  public RecordingTextTranslationGateway(AiRealtimeProperties properties) {
    this.properties = properties;
  }

  private record Result(String text) {}

  public String translate(String text) {
    if (properties.getServiceSecret() == null || properties.getServiceSecret().isBlank())
      throw new AiRuntimeIntegrationException("AI service is not configured", null);
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    factory.setReadTimeout(Duration.ofMillis(Math.max(1000, properties.getRequestTimeoutMs())));
    try {
      var result =
          RestClient.builder()
              .baseUrl(properties.getBaseUrl())
              .requestFactory(factory)
              .defaultHeader("Authorization", "Bearer " + properties.getServiceSecret())
              .build()
              .post()
              .uri("/internal/v1/recording-text/translate")
              .body(Map.of("text", text))
              .retrieve()
              .body(Result.class);
      if (result == null
          || result.text() == null
          || result.text().isBlank()
          || result.text().length() > 8000)
        throw new AiRuntimeIntegrationException("AI returned invalid translation", null);
      return result.text();
    } catch (RestClientException e) {
      throw new AiRuntimeIntegrationException("AI text translation unavailable", null);
    }
  }
}
