package org.ptit.meeting.integration.livekit;

import io.livekit.server.LiveKitAPI;
import io.livekit.server.WebhookReceiver;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.ptit.meeting.config.LiveKitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
  public LiveKitAPI liveKitApi(LiveKitProperties properties) {
    validateEnabledConfiguration(properties);
    return LiveKitAPI.createClient(
        properties.getApiUrl(),
        properties.getApiKey(),
        properties.getApiSecret()
    );
  }

  @Bean
  @ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
  public LiveKitTokenFactory liveKitTokenFactory(LiveKitProperties properties) {
    validateEnabledConfiguration(properties);
    return new LiveKitTokenFactory(properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
  public LiveKitGateway liveKitGateway(
      LiveKitAPI liveKitApi,
      LiveKitTokenFactory tokenFactory
  ) {
    return new LiveKitGatewayImpl(liveKitApi, tokenFactory);
  }

  @Bean
  @ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
  public WebhookReceiver liveKitWebhookReceiver(LiveKitProperties properties) {
    validateEnabledConfiguration(properties);
    return new WebhookReceiver(properties.getApiKey(), properties.getApiSecret());
  }

  @Bean
  @ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
  public LiveKitWebhookVerifier liveKitWebhookVerifier(
      WebhookReceiver webhookReceiver,
      LiveKitProperties properties
  ) {
    return new LiveKitWebhookVerifierImpl(webhookReceiver, properties);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "application.livekit",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true
  )
  public LiveKitGateway disabledLiveKitGateway() {
    return new DisabledLiveKitGateway();
  }

  private void validateEnabledConfiguration(LiveKitProperties properties) {
    requireText(properties.getApiUrl(), "LIVEKIT_API_URL is required");
    requireUrlScheme(properties.getApiUrl(), List.of("http", "https"), "LIVEKIT_API_URL");
    requireText(properties.getWebsocketUrl(), "LIVEKIT_WS_URL is required");
    requireUrlScheme(properties.getWebsocketUrl(), List.of("ws", "wss"), "LIVEKIT_WS_URL");
    requireText(properties.getApiKey(), "LIVEKIT_API_KEY is required");
    requireText(properties.getApiSecret(), "LIVEKIT_API_SECRET is required");
    if (properties.getJoinTokenTtlMs() <= 0) {
      throw new IllegalStateException("LIVEKIT_JOIN_TOKEN_TTL_MS must be positive");
    }
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(message);
    }
  }

  private void requireUrlScheme(String value, List<String> schemes, String propertyName) {
    try {
      URI uri = new URI(value);
      if (uri.getHost() == null || !schemes.contains(uri.getScheme())) {
        throw new IllegalStateException(propertyName + " has an invalid URL");
      }
    } catch (URISyntaxException exception) {
      throw new IllegalStateException(propertyName + " has an invalid URL", exception);
    }
  }
}
