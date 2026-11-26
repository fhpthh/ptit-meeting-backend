package org.ptit.meeting.modules.ai.dto.response;

/** Internal receipt; COMPLETED is an AI draft, not approval or student publication. */
public record RecordingTranslationResultResponse(
    String jobId,
    String runId,
    String draftId,
    long documentRevision,
    long durationMs,
    int viSegments,
    int enSegments) {}
