package org.ptit.meeting.modules.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;
import org.ptit.meeting.modules.ai.service.RecordingResultVerificationService;
import org.ptit.meeting.modules.ai.service.RecordingTranslationDraftService;
import org.ptit.meeting.modules.ai.service.RecordingTranslationResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingTranslationResultServiceImpl implements RecordingTranslationResultService {
  private final RecordingTranslationDraftService drafts;
  private final RecordingResultVerificationService verification;

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public RecordingTranslationResultResponse complete(
      String jobId, RecordingWorkerIdentity worker, RecordingResultCompleteRequest request) {
    var snapshot = drafts.authorize(jobId, worker, request);
    if (snapshot.existing() != null) return snapshot.existing();
    var result = verification.verify(snapshot.context(), request);
    return drafts.accept(jobId, worker, request, result);
  }
}
