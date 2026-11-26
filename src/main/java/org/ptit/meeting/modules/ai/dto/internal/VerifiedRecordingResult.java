package org.ptit.meeting.modules.ai.dto.internal;

import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;

/** Created after readback verification, then fenced again before the DB write. */
public record VerifiedRecordingResult(
    RecordingJobContextResponse context,
    String manifestJson,
    String documentJson,
    String sourceSha256,
    long documentRevision,
    long durationMs,
    int viSegments,
    int enSegments) {
  @Override
  public String toString() {
    return "VerifiedRecordingResult[content redacted]";
  }
}
