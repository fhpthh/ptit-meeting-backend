package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.internal.RecordingCompletionSnapshot;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.internal.VerifiedRecordingResult;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;

public interface RecordingTranslationDraftService {
  RecordingCompletionSnapshot authorize(
      String jobId, RecordingWorkerIdentity worker, RecordingResultCompleteRequest request);

  RecordingTranslationResultResponse accept(
      String jobId,
      RecordingWorkerIdentity worker,
      RecordingResultCompleteRequest request,
      VerifiedRecordingResult result);
}
