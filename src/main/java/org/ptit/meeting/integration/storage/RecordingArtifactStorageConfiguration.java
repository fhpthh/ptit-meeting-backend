package org.ptit.meeting.integration.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.ptit.meeting.config.AiRecordingProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "application.ai-recording.artifact-storage",
    name = "enabled",
    havingValue = "true")
public class RecordingArtifactStorageConfiguration {
  @Bean(destroyMethod = "close")
  public S3Client recordingArtifactS3Client(AiRecordingProperties properties) {
    var storage = properties.getArtifactStorage();
    if (storage.getBucket() == null
        || !storage.getBucket().matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")
        || storage.getAccessKey() == null
        || storage.getAccessKey().isBlank()
        || storage.getSecretKey() == null
        || storage.getSecretKey().isBlank()
        || storage.getRegion() == null
        || storage.getRegion().isBlank())
      throw new IllegalStateException("Recording artifact storage configuration is required");
    URI endpoint;
    try {
      endpoint = URI.create(storage.getEndpoint());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new IllegalStateException("Invalid recording artifact endpoint");
    }
    if (endpoint.getHost() == null
        || endpoint.getUserInfo() != null
        || endpoint.getQuery() != null
        || endpoint.getFragment() != null
        || !("http".equals(endpoint.getScheme()) || "https".equals(endpoint.getScheme()))
        || !(endpoint.getPath().isEmpty() || "/".equals(endpoint.getPath())))
      throw new IllegalStateException("Invalid recording artifact endpoint");
    return S3Client.builder()
        .endpointOverride(endpoint)
        .region(Region.of(storage.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.getAccessKey(), storage.getSecretKey())))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .overrideConfiguration(
            config ->
                config
                    .apiCallTimeout(Duration.ofSeconds(30))
                    .apiCallAttemptTimeout(Duration.ofSeconds(10)))
        .httpClientBuilder(
            UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(5))
                .socketTimeout(Duration.ofSeconds(10)))
        .build();
  }

  @Bean
  public RecordingArtifactStorage recordingArtifactStorage(
      @Qualifier("recordingArtifactS3Client") S3Client recordingArtifactS3Client,
      AiRecordingProperties properties,
      ObjectMapper json) {
    return new S3RecordingArtifactStorage(
        recordingArtifactS3Client, properties.getArtifactStorage().getBucket(), json);
  }
}
