package org.ptit.meeting.modules.meeting.constant.enums;

public enum MeetingRecordingStatus {
  ACTIVE,
  FINALIZING,
  COMPLETED,
  FAILED,
  ABORTED;

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == ABORTED;
  }
}
