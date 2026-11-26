package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.internal.VerifiedRecordingResult;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;

public interface RecordingResultVerificationService {
  VerifiedRecordingResult verify(
      RecordingJobContextResponse context, RecordingResultCompleteRequest request);
}
