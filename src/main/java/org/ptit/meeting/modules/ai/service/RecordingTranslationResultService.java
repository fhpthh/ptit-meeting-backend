package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;

public interface RecordingTranslationResultService {
  RecordingTranslationResultResponse complete(
      String jobId, RecordingWorkerIdentity worker, RecordingResultCompleteRequest request);
}
