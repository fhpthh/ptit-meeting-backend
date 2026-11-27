package org.ptit.meeting.modules.ai.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.CheckpointRequest;

public record RecordingWorkerGrantResponse(
    RecordingJobContextResponse context,
    CheckpointRequest checkpoint,
    Instant leaseUntil,
    String accessToken,
    Instant tokenExpiresAt) {
  @Override
  public String toString() {
    return "RecordingWorkerGrantResponse[credentials redacted]";
  }
}
