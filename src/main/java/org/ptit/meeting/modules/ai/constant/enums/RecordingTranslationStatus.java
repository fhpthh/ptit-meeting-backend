package org.ptit.meeting.modules.ai.constant.enums;

public enum RecordingTranslationStatus {
  QUEUED,
  PROCESSING,
  PAUSED_RESOURCE,
  RETRY_WAIT,
  COMPLETED,
  FAILED,
  CANCELLED;

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
  }
}
