package org.ptit.meeting.modules.ai.dto.internal;

public record RecordingDispatchSnapshot(
    String eventId,
    String jobId,
    String recordingId,
    String runId,
    long recordingEpoch,
    long dispatchVersion) {}
