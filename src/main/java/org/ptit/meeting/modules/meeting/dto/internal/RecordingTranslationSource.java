package org.ptit.meeting.modules.meeting.dto.internal;

/** Internal service value; never returned by a public recording controller. */
public record RecordingTranslationSource(
    String recordingId,
    String runtimeSessionId,
    String meetingRoomId,
    String bucket,
    String objectKey,
    long sizeBytes,
    Long classSectionId) {}
