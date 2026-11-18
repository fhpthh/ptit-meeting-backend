package org.ptit.meeting.integration.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.ptit.meeting.config.AiRealtimeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiRuntimeConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "application.ai-realtime", name = "enabled", havingValue = "true")
  AiRuntimeGateway aiRuntimeGateway(AiRealtimeProperties properties) {
    if (properties.getServiceSecret() == null || properties.getServiceSecret().isBlank()) {
      throw new IllegalStateException("AI_REALTIME_SERVICE_SECRET is required");
    }
    // The Python control plane is served by Uvicorn over HTTP/1.1.  The JDK client
    // otherwise attempts an h2c upgrade, which Uvicorn correctly rejects before
    // the start command reaches the session registry.
    HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(properties.getRequestTimeoutMs()));
    RestClient client = RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + properties.getServiceSecret())
        .build();
    return new HttpAiRuntimeGateway(client);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "application.ai-realtime",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true)
  AiRuntimeGateway disabledAiRuntimeGateway() {
    return new AiRuntimeGateway() {
      private AiRuntimeIntegrationException disabled() {
        return new AiRuntimeIntegrationException("AI realtime is disabled", null);
      }

      @Override
      public org.ptit.meeting.integration.ai.dto.AiRuntimeView start(
          org.ptit.meeting.integration.ai.dto.AiRuntimeStartCommand command) {
        throw disabled();
      }

      @Override
      public org.ptit.meeting.integration.ai.dto.AiRuntimeView stop(String runtimeSessionId) {
        throw disabled();
      }

      @Override
      public org.ptit.meeting.integration.ai.dto.AiRuntimeView status(String runtimeSessionId) {
        throw disabled();
      }
    };
  }
}
