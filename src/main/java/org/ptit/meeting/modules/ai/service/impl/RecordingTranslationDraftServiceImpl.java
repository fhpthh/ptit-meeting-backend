package org.ptit.meeting.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStage;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;
import org.ptit.meeting.modules.ai.dto.internal.RecordingCompletionSnapshot;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.internal.VerifiedRecordingResult;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationDraft;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationJob;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationDraftRepository;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationJobRepository;
import org.ptit.meeting.modules.ai.service.RecordingTranslationDraftService;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingTranslationDraftServiceImpl implements RecordingTranslationDraftService {
  private final RecordingTranslationJobRepository jobs;
  private final RecordingTranslationDraftRepository drafts;
  private final MeetingRecordingService recordings;
  private final UserService users;
  private final AiRecordingProperties properties;
  private final ObjectMapper json;

  @Override
  @Transactional(readOnly = true)
  public RecordingCompletionSnapshot authorize(
      String jobId, RecordingWorkerIdentity worker, RecordingResultCompleteRequest request) {
    requireEnabled();
    var job =
        jobs.findById(jobId)
            .orElseThrow(
                () -> BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND));
    identity(job, worker);
    requireReference(job, request);
    requireLecturer(job.getRequestedByUserId());
    var existing = completed(job, request);
    if (existing == null) active(job);
    return new RecordingCompletionSnapshot(context(job), existing);
  }

  @Override
  // Under MySQL REPEATABLE_READ an earlier authority snapshot could hide a draft
  // committed by the callback that held the job lock immediately before us.
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RecordingTranslationResultResponse accept(
      String jobId,
      RecordingWorkerIdentity worker,
      RecordingResultCompleteRequest request,
      VerifiedRecordingResult result) {
    requireEnabled();
    var snapshot =
        jobs.findCompletionAuthority(jobId)
            .orElseThrow(
                () -> BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND));
    requireLecturer(snapshot.requestedByUserId());
    // Same recording -> job lock order as create/cancel/claim.
    var source =
        recordings.requireTranslationSource(snapshot.recordingId(), snapshot.requestedByUserId());
    var job =
        jobs.findByIdForUpdate(jobId)
            .orElseThrow(
                () -> BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND));
    identity(job, worker);
    if (!job.getRecordingId().equals(snapshot.recordingId())
        || !job.getRequestedByUserId().equals(snapshot.requestedByUserId())) throw conflict();
    requireReference(job, request);
    var existing = completed(job, request);
    if (existing != null) return existing;
    active(job);
    if (!source.bucket().equals(job.getSourceBucket())
        || !source.objectKey().equals(job.getSourceObjectKey())
        || source.sizeBytes() != job.getSourceSizeBytes()) throw conflict();
    var expected = context(job);
    var supplied = result.context();
    if (!expected.attempt().equals(supplied.attempt())
        || !expected.profile().equals(supplied.profile())
        || !Objects.equals(expected.courseContext(), supplied.courseContext())
        || !expected.source().bucket().equals(supplied.source().bucket())
        || !expected.source().objectKey().equals(supplied.source().objectKey())
        || expected.source().sizeBytes() != supplied.source().sizeBytes()
        || (job.getSourceSha256() != null && !job.getSourceSha256().equals(result.sourceSha256())))
      throw conflict();
    var draft = new RecordingTranslationDraft();
    draft.setJobId(jobId);
    draft.setRecordingId(job.getRecordingId());
    draft.setRunId(job.getRunId());
    draft.setRecordingEpoch(job.getRecordingEpoch());
    draft.setAttemptId(job.getAttemptId());
    draft.setFencingToken(job.getFencingToken());
    draft.setManifestBucket(request.bucket());
    draft.setManifestKey(request.objectKey());
    draft.setManifestSha256(request.sha256());
    draft.setManifestBytes(request.sizeBytes());
    draft.setManifestJson(result.manifestJson());
    draft.setOriginalDocumentJson(result.documentJson());
    draft.setSourceSha256(result.sourceSha256());
    draft.setDocumentRevision(result.documentRevision());
    draft.setDurationMs(result.durationMs());
    draft.setViSegments(result.viSegments());
    draft.setEnSegments(result.enSegments());
    drafts.saveAndFlush(draft);
    active(job); // A long DB write must not turn an expired lease into success.
    job.setStatus(RecordingTranslationStatus.COMPLETED);
    job.setStage(RecordingTranslationStage.PERSIST);
    job.setProgressPercent(100);
    job.setLeaseUntil(null);
    job.setActiveSlot(null);
    job.setErrorCode(null);
    jobs.saveAndFlush(job);
    return response(draft);
  }

  private void requireEnabled() {
    if (!properties.isEnabled() || !properties.isCompletionEnabled())
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_DISABLED);
  }

  private void requireLecturer(Long userId) {
    var user = users.getUserById(userId);
    users.validateUserStatus(user);
    if (user.getAccountType() != AccountType.LECTURER)
      throw BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
  }

  private void identity(RecordingTranslationJob job, RecordingWorkerIdentity worker) {
    if (!"attempt".equals(worker.scope()) || !job.getId().equals(worker.jobId()))
      throw BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
    if (!Objects.equals(job.getWorkerId(), worker.workerId())
        || !Objects.equals(job.getAttemptId(), worker.attemptId())
        || !job.getRecordingId().equals(worker.recordingId())
        || !job.getRunId().equals(worker.runId())
        || job.getRecordingEpoch() != worker.recordingEpoch()
        || job.getFencingToken() != worker.fencingToken()) throw conflict();
  }

  private void active(RecordingTranslationJob job) {
    if (job.getStatus() != RecordingTranslationStatus.PROCESSING
        || job.getLeaseUntil() == null
        || !job.getLeaseUntil().isAfter(Instant.now())) throw conflict();
  }

  private void requireReference(
      RecordingTranslationJob job, RecordingResultCompleteRequest request) {
    String prefix =
        "recording-translations/"
            + job.getRecordingId()
            + "/runs/"
            + job.getRunId()
            + "/jobs/"
            + job.getId()
            + "/epochs/"
            + job.getRecordingEpoch()
            + "/attempts/"
            + job.getAttemptId()
            + "/fences/"
            + job.getFencingToken()
            + "/";
    if (request.bucket() == null
        || !Objects.equals(properties.getArtifactStorage().getBucket(), request.bucket())
        || request.bucket().equals(job.getSourceBucket())
        || !Objects.equals(request.objectKey(), prefix + "result-manifest.json")
        || request.sha256() == null
        || !request.sha256().matches("[a-f0-9]{64}")
        || request.sizeBytes() == null
        || request.sizeBytes() < 1
        || request.sizeBytes() > 262144)
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_RESULT_INVALID);
  }

  private RecordingTranslationResultResponse completed(
      RecordingTranslationJob job, RecordingResultCompleteRequest request) {
    if (job.getStatus() != RecordingTranslationStatus.COMPLETED) return null;
    var draft = drafts.findByJobId(job.getId()).orElseThrow(this::conflict);
    if (!draft.getManifestBucket().equals(request.bucket())
        || !draft.getManifestKey().equals(request.objectKey())
        || !draft.getManifestSha256().equals(request.sha256())
        || draft.getManifestBytes() != request.sizeBytes()) throw conflict();
    return response(draft);
  }

  private RecordingTranslationResultResponse response(RecordingTranslationDraft draft) {
    return new RecordingTranslationResultResponse(
        draft.getJobId(),
        draft.getRunId(),
        draft.getId(),
        draft.getDocumentRevision(),
        draft.getDurationMs(),
        draft.getViSegments(),
        draft.getEnSegments());
  }

  private RecordingJobContextResponse context(RecordingTranslationJob job) {
    try {
      return new RecordingJobContextResponse(
          1,
          "recording.job-context",
          new RecordingJobContextResponse.Attempt(
              job.getRecordingId(),
              job.getId(),
              job.getRunId(),
              job.getRecordingEpoch(),
              job.getAttemptId(),
              job.getFencingToken()),
          new RecordingJobContextResponse.Source(
              job.getSourceBucket(),
              job.getSourceObjectKey(),
              job.getSourceSizeBytes(),
              job.getSourceSha256()),
          new RecordingJobContextResponse.Profile(
              job.getProfileId(),
              job.getProfileVersion(),
              job.getSettingsSha256(),
              job.getModelManifestSha256()),
          json.readValue(
              job.getCourseContextJson(), RecordingJobContextResponse.CourseContext.class));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid persisted recording context");
    }
  }

  private BusinessException conflict() {
    return new BusinessException(StatusCode.CONFLICT, AiErrorConstants.RECORDING_CONFLICT);
  }
}
