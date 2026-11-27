package org.ptit.meeting.modules.ai.dto.internal;

public record RecordingWorkerIdentity(
    String jobId,
    String workerId,
    String scope,
    String recordingId,
    String runId,
    long recordingEpoch,
    String attemptId,
    long fencingToken) {}
