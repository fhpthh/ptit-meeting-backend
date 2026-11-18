package org.ptit.meeting.integration.ai.dto;

public record AiRuntimeStartCommand(
    int schemaVersion,
    String runtimeSessionId,
    long enableEpoch,
    String providerRoomName,
    String roomPublicId,
    String lecturerIdentity,
    String lecturerUserId,
    String livekitUrl,
    String livekitToken,
    String finalCaptionCallbackUrl,
    String finalCaptionCallbackSecret,
    AiRuntimeCourseContext courseContext
) {
  public AiRuntimeStartCommand(
      int schemaVersion, String runtimeSessionId, long enableEpoch, String providerRoomName,
      String roomPublicId, String lecturerIdentity, String lecturerUserId, String livekitUrl,
      String livekitToken, String finalCaptionCallbackUrl, String finalCaptionCallbackSecret
  ) {
    this(schemaVersion, runtimeSessionId, enableEpoch, providerRoomName, roomPublicId,
        lecturerIdentity, lecturerUserId, livekitUrl, livekitToken, finalCaptionCallbackUrl,
        finalCaptionCallbackSecret, null);
  }

  @Override
  public String toString() {
    return "AiRuntimeStartCommand["
        + "schemaVersion=" + schemaVersion
        + ", runtimeSessionId=" + runtimeSessionId
        + ", enableEpoch=" + enableEpoch
        + ", providerRoomName=" + providerRoomName
        + ", roomPublicId=" + roomPublicId
        + ", lecturerIdentity=" + lecturerIdentity
        + ", lecturerUserId=" + lecturerUserId
        + ", livekitUrl=" + livekitUrl
        + ", livekitToken=<redacted>"
        + ", finalCaptionCallbackUrl=" + finalCaptionCallbackUrl
        + ", finalCaptionCallbackSecret=<redacted>]";
  }
}
