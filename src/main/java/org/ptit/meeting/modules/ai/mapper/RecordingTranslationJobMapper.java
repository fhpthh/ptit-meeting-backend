package org.ptit.meeting.modules.ai.mapper;

import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationJobResponse;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationJob;
import org.springframework.stereotype.Component;

@Component
public class RecordingTranslationJobMapper {
  public RecordingTranslationJobResponse toResponse(RecordingTranslationJob job) {
    return new RecordingTranslationJobResponse(
        job.getId(),
        job.getRecordingId(),
        job.getRunId(),
        job.getStatus(),
        job.getStage(),
        job.getProgressPercent(),
        job.getAttempt(),
        job.getErrorCode(),
        job.getCreatedAt(),
        job.getUpdatedAt());
  }
}
