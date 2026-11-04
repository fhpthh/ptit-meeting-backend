package org.ptit.meeting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.recording")
public class RecordingProperties {

  private boolean enabled;
  private long cleanupIntervalMs = 120_000;
  private long staleUploadTimeoutMs = 900_000;
  private long uploadPartUrlTtlMs = 900_000;
  private Storage storage = new Storage();

  @Getter
  @Setter
  public static class Storage {

    private String bucket;
    private String region = "us-east-1";
    private String publicBaseUrl;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean forcePathStyle = true;
  }
}
