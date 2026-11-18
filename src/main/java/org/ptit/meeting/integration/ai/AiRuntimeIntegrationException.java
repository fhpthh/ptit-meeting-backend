package org.ptit.meeting.integration.ai;

public class AiRuntimeIntegrationException extends RuntimeException {

  private final Integer httpStatus;

  public AiRuntimeIntegrationException(String message, Throwable cause) {
    this(message, cause, null);
  }

  public AiRuntimeIntegrationException(String message, Throwable cause, Integer httpStatus) {
    super(message, cause);
    this.httpStatus = httpStatus;
  }

  public boolean isNotFound() {
    return Integer.valueOf(404).equals(httpStatus);
  }
}
