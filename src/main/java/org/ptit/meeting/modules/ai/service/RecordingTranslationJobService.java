package org.ptit.meeting.modules.ai.service;

import java.util.List;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingTranslationCreateRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ClaimRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.FailureRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ProgressRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationJobResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;

public interface RecordingTranslationJobService {
  RecordingTranslationJobResponse create(
      String recordingId,
      Long userId,
      String idempotencyKey,
      RecordingTranslationCreateRequest request);

  List<RecordingTranslationJobResponse> list(String recordingId, Long userId);

  RecordingTranslationJobResponse get(String recordingId, String jobId, Long userId);

  RecordingTranslationJobResponse cancel(String recordingId, String jobId, Long userId);

  RecordingWorkerGrantResponse claim(
      String jobId, RecordingWorkerIdentity worker, ClaimRequest request);

  RecordingWorkerGrantResponse heartbeat(String jobId, RecordingWorkerIdentity worker);

  RecordingWorkerGrantResponse progress(
      String jobId, RecordingWorkerIdentity worker, ProgressRequest request);

  RecordingTranslationJobResponse fail(
      String jobId, RecordingWorkerIdentity worker, FailureRequest request);
}
