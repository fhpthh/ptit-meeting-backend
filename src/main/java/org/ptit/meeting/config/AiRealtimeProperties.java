package org.ptit.meeting.config;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.ai-realtime")
public class AiRealtimeProperties {

  private boolean enabled;
  private String baseUrl = "http://127.0.0.1:8020";
  private String serviceSecret;
  private String callbackBaseUrl = "http://127.0.0.1:8080";
  private long requestTimeoutMs = 120_000;
  private String terminologyProfile = "whisper_ai";
  private Map<String, String> terminologyProfiles = new HashMap<>();
}
