package org.ptit.meeting.integration.storage;

import java.time.Instant;

public interface MeetingRecordingStorage {

  String createMultipartUpload(String objectKey, String mimeType);

  UploadPartGrant createUploadPartGrant(
      String objectKey,
      String uploadId,
      int partNumber
  );

  StoredObject completeMultipartUpload(String objectKey, String uploadId);

  void abortMultipartUpload(String objectKey, String uploadId);

  record UploadPartGrant(String url, Instant expiresAt) {
  }

  record StoredObject(long sizeBytes) {
  }

  final class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
      super(message, cause);
    }

    public StorageException(String message) {
      super(message);
    }
  }
}
