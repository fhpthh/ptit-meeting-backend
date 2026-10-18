package org.ptit.meeting.integration.livekit;

public final class LiveKitIntegrationException extends RuntimeException {

  public LiveKitIntegrationException(String message) {
    super(message);
  }

  public LiveKitIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
