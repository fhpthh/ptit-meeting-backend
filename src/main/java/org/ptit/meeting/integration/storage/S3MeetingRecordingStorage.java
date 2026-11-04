package org.ptit.meeting.integration.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

final class S3MeetingRecordingStorage implements MeetingRecordingStorage {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String bucket;
  private final Duration uploadPartUrlTtl;

  S3MeetingRecordingStorage(
      S3Client s3Client,
      S3Presigner s3Presigner,
      String bucket,
      Duration uploadPartUrlTtl
  ) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.bucket = bucket;
    this.uploadPartUrlTtl = uploadPartUrlTtl;
  }

  @Override
  public String createMultipartUpload(String objectKey, String mimeType) {
    try {
      return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .contentType(mimeType)
              .build())
          .uploadId();
    } catch (SdkException exception) {
      throw new StorageException("Unable to create recording upload", exception);
    }
  }

  @Override
  public UploadPartGrant createUploadPartGrant(
      String objectKey,
      String uploadId,
      int partNumber
  ) {
    try {
      UploadPartRequest uploadRequest = UploadPartRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .uploadId(uploadId)
          .partNumber(partNumber)
          .build();
      PresignedUploadPartRequest presigned = s3Presigner.presignUploadPart(
          UploadPartPresignRequest.builder()
              .signatureDuration(uploadPartUrlTtl)
              .uploadPartRequest(uploadRequest)
              .build()
      );
      return new UploadPartGrant(
          presigned.url().toString(),
          Instant.now().plus(uploadPartUrlTtl)
      );
    } catch (SdkException exception) {
      throw new StorageException("Unable to authorize recording upload part", exception);
    }
  }

  @Override
  public StoredObject completeMultipartUpload(String objectKey, String uploadId) {
    try {
      List<Part> uploadedParts = listAllParts(objectKey, uploadId);
      validateParts(uploadedParts);
      long sizeBytes = uploadedParts.stream().mapToLong(Part::size).sum();
      List<CompletedPart> completedParts = uploadedParts.stream()
          .map(part -> CompletedPart.builder()
              .partNumber(part.partNumber())
              .eTag(part.eTag())
              .build())
          .toList();

      s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .uploadId(uploadId)
          .multipartUpload(CompletedMultipartUpload.builder()
              .parts(completedParts)
              .build())
          .build());
      return new StoredObject(sizeBytes);
    } catch (StorageException exception) {
      throw exception;
    } catch (SdkException exception) {
      StoredObject completedObject = findCompletedObject(objectKey, exception);
      if (completedObject != null) {
        return completedObject;
      }
      throw new StorageException("Unable to complete recording upload", exception);
    }
  }

  @Override
  public void abortMultipartUpload(String objectKey, String uploadId) {
    try {
      s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .uploadId(uploadId)
          .build());
    } catch (SdkException exception) {
      throw new StorageException("Unable to abort recording upload", exception);
    }
  }

  private List<Part> listAllParts(String objectKey, String uploadId) {
    List<Part> parts = new ArrayList<>();
    Integer marker = null;
    boolean truncated;
    do {
      ListPartsResponse response = s3Client.listParts(ListPartsRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .uploadId(uploadId)
          .partNumberMarker(marker)
          .build());
      parts.addAll(response.parts());
      truncated = Boolean.TRUE.equals(response.isTruncated());
      marker = response.nextPartNumberMarker();
    } while (truncated);
    parts.sort(Comparator.comparingInt(Part::partNumber));
    return parts;
  }

  private void validateParts(List<Part> parts) {
    if (parts.isEmpty()) {
      throw new StorageException("Recording upload has no parts");
    }
    for (int index = 0; index < parts.size(); index++) {
      if (parts.get(index).partNumber() != index + 1) {
        throw new StorageException("Recording upload parts are incomplete");
      }
    }
  }

  private StoredObject findCompletedObject(String objectKey, SdkException completionFailure) {
    try {
      long sizeBytes = s3Client.headObject(HeadObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .build())
          .contentLength();
      return new StoredObject(sizeBytes);
    } catch (SdkException lookupFailure) {
      completionFailure.addSuppressed(lookupFailure);
      return null;
    }
  }
}
