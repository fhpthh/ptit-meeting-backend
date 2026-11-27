package org.ptit.meeting.modules.ai.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStage;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;

/** Public view: never includes storage keys, worker credentials or checkpoints. */
public record RecordingTranslationJobResponse(
    String jobId,
    String recordingId,
    String runId,
    RecordingTranslationStatus status,
    RecordingTranslationStage stage,
    int progressPercent,
    int attempt,
    String errorCode,
    Instant createdAt,
    Instant updatedAt) {}
