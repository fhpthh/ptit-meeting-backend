package org.ptit.meeting.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.ai-recording")
public class AiRecordingProperties {
  private boolean enabled;
  private boolean completionEnabled;
  private ArtifactStorage artifactStorage = new ArtifactStorage();
  private String workerSecret;
  private long leaseSeconds = 120;
  private long tokenTtlSeconds = 600;
  private int maxAttempts = 3;
  private long retryDelaySeconds = 30;
  private String profileId = "saved-media-v1";
  private int profileVersion = 1;
  private String settingsSha256;
  private String modelManifestSha256;
  private String terminologyProfile = "whisper_ai";
  private Map<String, String> terminologyProfiles = new HashMap<>();

  @Getter
  @Setter
  public static class ArtifactStorage {
    private boolean enabled;
    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
  }
}
