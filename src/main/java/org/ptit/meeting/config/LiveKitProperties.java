package org.ptit.meeting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.livekit")
public class LiveKitProperties {

  private boolean enabled;
  private String apiUrl;
  private String websocketUrl;
  private String apiKey;
  private String apiSecret;
  private long joinTokenTtlMs = 600_000;
}
