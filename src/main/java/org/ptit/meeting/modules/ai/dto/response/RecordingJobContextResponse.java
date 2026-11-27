package org.ptit.meeting.modules.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Shape mirrored by ptit_ai/contracts/v1/schemas/recording.schema.json. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RecordingJobContextResponse(
    int schemaVersion,
    String type,
    Attempt attempt,
    Source source,
    Profile profile,
    CourseContext courseContext) {
  public record Attempt(
      String recordingId,
      String jobId,
      String runId,
      long recordingEpoch,
      String attemptId,
      long fencingToken) {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  public record Source(String bucket, String objectKey, long sizeBytes, String expectedSha256) {}

  public record Profile(
      String profileId, int version, String settingsSha256, String modelManifestSha256) {}

  public record CourseContext(
      String subjectCode,
      String subjectName,
      String terminologyProfileId,
      int terminologyProfileVersion) {}
}
