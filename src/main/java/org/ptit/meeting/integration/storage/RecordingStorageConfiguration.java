package org.ptit.meeting.integration.storage;

import java.net.URI;
import java.time.Duration;
import org.ptit.meeting.config.RecordingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(
    prefix = "application.recording",
    name = "enabled",
    havingValue = "true"
)
public class RecordingStorageConfiguration {

  @Bean(destroyMethod = "close")
  public S3Client recordingS3Client(RecordingProperties properties) {
    StorageSettings settings = validate(properties);
    return S3Client.builder()
        .endpointOverride(settings.endpoint())
        .region(settings.region())
        .credentialsProvider(settings.credentials())
        .serviceConfiguration(settings.serviceConfiguration())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner recordingS3Presigner(RecordingProperties properties) {
    StorageSettings settings = validate(properties);
    return S3Presigner.builder()
        .endpointOverride(settings.endpoint())
        .region(settings.region())
        .credentialsProvider(settings.credentials())
        .serviceConfiguration(settings.serviceConfiguration())
        .build();
  }

  @Bean
  public MeetingRecordingStorage meetingRecordingStorage(
      S3Client recordingS3Client,
      S3Presigner recordingS3Presigner,
      RecordingProperties properties
  ) {
    return new S3MeetingRecordingStorage(
        recordingS3Client,
        recordingS3Presigner,
        properties.getStorage().getBucket().trim(),
        Duration.ofMillis(Math.max(1_000, properties.getUploadPartUrlTtlMs()))
    );
  }

  private StorageSettings validate(RecordingProperties properties) {
    RecordingProperties.Storage storage = properties.getStorage();
    requireText(storage.getEndpoint(), "RECORDING_STORAGE_ENDPOINT is required");
    requireText(storage.getBucket(), "RECORDING_STORAGE_BUCKET is required");
    requireText(storage.getRegion(), "RECORDING_STORAGE_REGION is required");
    requireText(storage.getAccessKey(), "RECORDING_STORAGE_ACCESS_KEY is required");
    requireText(storage.getSecretKey(), "RECORDING_STORAGE_SECRET_KEY is required");

    URI endpoint;
    try {
      endpoint = URI.create(storage.getEndpoint().trim());
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("RECORDING_STORAGE_ENDPOINT is invalid", exception);
    }
    if (endpoint.getHost() == null
        || !("http".equals(endpoint.getScheme()) || "https".equals(endpoint.getScheme()))) {
      throw new IllegalStateException("RECORDING_STORAGE_ENDPOINT is invalid");
    }

    return new StorageSettings(
        endpoint,
        Region.of(storage.getRegion().trim()),
        StaticCredentialsProvider.create(AwsBasicCredentials.create(
            storage.getAccessKey().trim(),
            storage.getSecretKey().trim()
        )),
        S3Configuration.builder()
            .pathStyleAccessEnabled(storage.isForcePathStyle())
            .build()
    );
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(message);
    }
  }

  private record StorageSettings(
      URI endpoint,
      Region region,
      StaticCredentialsProvider credentials,
      S3Configuration serviceConfiguration
  ) {
  }
}
