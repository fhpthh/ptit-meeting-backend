package org.ptit.meeting.integration.storage;

/** Read-only technical boundary. Job/fence/manifest semantics remain in the AI module. */
public interface RecordingArtifactStorage {
  record ArtifactObject(String key, String sha256, long sizeBytes, String contentType) {}

  void requirePrivate();

  byte[] readVerified(ArtifactObject object);

  void verify(ArtifactObject object);

  class ArtifactMismatchException extends RuntimeException {
    public ArtifactMismatchException() {
      super("Recording artifact verification failed");
    }
  }

  class StorageException extends RuntimeException {
    public StorageException() {
      super("Recording artifact storage unavailable");
    }
  }
}
