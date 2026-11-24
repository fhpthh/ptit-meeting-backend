package org.ptit.meeting.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStage;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingTranslationCreateRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.CheckpointRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ClaimRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.FailureRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ProgressRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse.CourseContext;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationJobResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;
import org.ptit.meeting.modules.ai.entity.RecordingJobOutbox;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationJob;
import org.ptit.meeting.modules.ai.mapper.RecordingTranslationJobMapper;
import org.ptit.meeting.modules.ai.repository.RecordingJobOutboxRepository;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationJobRepository;
import org.ptit.meeting.modules.ai.service.RecordingTranslationJobService;
import org.ptit.meeting.modules.ai.service.RecordingWorkerTokenService;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.dto.internal.RecordingTranslationSource;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.ptit.meeting.modules.timetable.service.ClassSectionSubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordingTranslationJobServiceImpl implements RecordingTranslationJobService {
  private final RecordingTranslationJobRepository jobs;
  private final RecordingJobOutboxRepository outbox;
  private final MeetingRecordingService recordings;
  private final UserService users;
  private final ClassSectionSubjectService subjects;
  private final AiRecordingProperties properties;
  private final RecordingWorkerTokenService tokens;
  private final RecordingTranslationJobMapper mapper;
  private final ObjectMapper json;

  @Override
  public RecordingTranslationJobResponse create(
      String recordingId, Long userId, String key, RecordingTranslationCreateRequest request) {
    requireEnabled();
    require(request != null && key != null && key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"));
    RecordingTranslationSource source = requireManager(recordingId, userId);
    String fingerprint = hash(json(request));
    var existing =
        jobs.findByRecordingIdAndRequestedByUserIdAndIdempotencyHash(
            recordingId, userId, hash(key));
    if (existing.isPresent()) {
      if (!existing.get().getRequestHash().equals(fingerprint)) throw conflict();
      return mapper.toResponse(existing.get());
    }
    requireConfiguration();
    if (source.bucket() == null || !source.bucket().matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_CONFIGURATION);
    }
    if (jobs.findByActiveSlot(recordingId).isPresent()) throw conflict();
    var previous = jobs.findFirstByRecordingIdOrderByCreatedAtDesc(recordingId);
    if (!Objects.equals(
        request.expectedRunId(), previous.map(RecordingTranslationJob::getRunId).orElse(null))) {
      throw conflict();
    }
    RecordingTranslationJob job = new RecordingTranslationJob();
    job.setId(UUID.randomUUID().toString());
    job.setRecordingId(recordingId);
    job.setRuntimeSessionId(source.runtimeSessionId());
    job.setRunId(UUID.randomUUID().toString());
    job.setRecordingEpoch(previous.map(value -> value.getRecordingEpoch() + 1).orElse(1L));
    job.setRequestedByUserId(userId);
    job.setIdempotencyHash(hash(key));
    job.setRequestHash(fingerprint);
    job.setActiveSlot(recordingId);
    job.setStatus(RecordingTranslationStatus.QUEUED);
    job.setStage(RecordingTranslationStage.DOWNLOAD);
    job.setMaxAttempts(properties.getMaxAttempts());
    job.setSourceBucket(source.bucket());
    job.setSourceObjectKey(source.objectKey());
    job.setSourceSizeBytes(source.sizeBytes());
    job.setProfileId(properties.getProfileId());
    job.setProfileVersion(properties.getProfileVersion());
    job.setSettingsSha256(properties.getSettingsSha256());
    job.setModelManifestSha256(properties.getModelManifestSha256());
    job.setCourseContextJson(json(courseContext(source)));
    jobs.saveAndFlush(job);
    outbox.save(new RecordingJobOutbox(job.getId(), 1, Instant.now()));
    return mapper.toResponse(job);
  }

  @Override
  public List<RecordingTranslationJobResponse> list(String recordingId, Long userId) {
    requireManager(recordingId, userId);
    return jobs.findTop50ByRecordingIdOrderByCreatedAtDesc(recordingId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public RecordingTranslationJobResponse get(String recordingId, String jobId, Long userId) {
    requireManager(recordingId, userId);
    return mapper.toResponse(findForRecording(recordingId, jobId));
  }

  @Override
  public RecordingTranslationJobResponse cancel(String recordingId, String jobId, Long userId) {
    requireManager(recordingId, userId);
    RecordingTranslationJob job = lock(jobId);
    if (!job.getRecordingId().equals(recordingId)) throw notFound();
    if (!job.getStatus().isTerminal()) {
      job.setStatus(RecordingTranslationStatus.CANCELLED);
      job.setFencingToken(job.getFencingToken() + 1);
      job.setLeaseUntil(null);
      job.setActiveSlot(null);
    }
    return mapper.toResponse(jobs.saveAndFlush(job));
  }

  @Override
  public RecordingWorkerGrantResponse claim(
      String jobId, RecordingWorkerIdentity worker, ClaimRequest request) {
    requireEnabled();
    requireConfiguration();
    if (!"claim".equals(worker.scope()) || !jobId.equals(worker.jobId())) throw denied();
    RecordingTranslationJob snapshot = jobs.findById(jobId).orElseThrow(this::notFound);
    // Same lock order as create/cancel: recording first, then job.
    requireManager(snapshot.getRecordingId(), snapshot.getRequestedByUserId());
    RecordingTranslationJob job = lock(jobId);
    Instant now = Instant.now();
    if (job.getStatus().isTerminal()) throw conflict();
    if (job.getStatus() == RecordingTranslationStatus.PROCESSING
        && job.getLeaseUntil().isAfter(now)) {
      if (!worker.workerId().equals(job.getWorkerId())
          || !request.requestId().equals(job.getClaimRequestId())) {
        throw conflict();
      }
      return grant(job);
    }
    if (job.getRetryAt() != null && job.getRetryAt().isAfter(now)) throw conflict();
    if (job.getAttempt() >= job.getMaxAttempts()) throw conflict();
    job.setAttempt(job.getAttempt() + 1);
    job.setAttemptId(UUID.randomUUID().toString());
    job.setWorkerId(worker.workerId());
    job.setClaimRequestId(request.requestId());
    job.setFencingToken(job.getFencingToken() + 1);
    job.setLeaseUntil(now.plusSeconds(properties.getLeaseSeconds()));
    job.setStatus(RecordingTranslationStatus.PROCESSING);
    job.setStage(RecordingTranslationStage.DOWNLOAD);
    job.setProgressPercent(0);
    job.setProgressSequence(0);
    job.setProgressHash(null);
    job.setRetryAt(null);
    job.setErrorCode(null);
    job.setFailureHash(null);
    jobs.saveAndFlush(job);
    return grant(job);
  }

  @Override
  public RecordingWorkerGrantResponse heartbeat(String jobId, RecordingWorkerIdentity worker) {
    RecordingTranslationJob job = requireAttempt(jobId, worker, true);
    job.setLeaseUntil(Instant.now().plusSeconds(properties.getLeaseSeconds()));
    return grant(jobs.saveAndFlush(job));
  }

  @Override
  public RecordingWorkerGrantResponse progress(
      String jobId, RecordingWorkerIdentity worker, ProgressRequest request) {
    RecordingTranslationJob job = requireAttempt(jobId, worker, true);
    String fingerprint = hash(json(request));
    if (request.sequence() <= job.getProgressSequence()) {
      if (request.sequence() == job.getProgressSequence()
          && !fingerprint.equals(job.getProgressHash())) {
        throw conflict();
      }
      return grant(job);
    }
    if (request.stage().ordinal() < job.getStage().ordinal()
        || request.percent() < job.getProgressPercent()) {
      throw conflict();
    }
    if (request.checkpoint() != null) saveCheckpoint(job, request.checkpoint());
    job.setProgressSequence(request.sequence());
    job.setProgressHash(fingerprint);
    job.setStage(request.stage());
    job.setProgressPercent(request.percent());
    job.setLeaseUntil(Instant.now().plusSeconds(properties.getLeaseSeconds()));
    return grant(jobs.saveAndFlush(job));
  }

  @Override
  public RecordingTranslationJobResponse fail(
      String jobId, RecordingWorkerIdentity worker, FailureRequest request) {
    RecordingTranslationJob job = requireAttempt(jobId, worker, false);
    String fingerprint = hash(json(request));
    if ((job.getStatus() == RecordingTranslationStatus.RETRY_WAIT
            || job.getStatus() == RecordingTranslationStatus.FAILED)
        && fingerprint.equals(job.getFailureHash())) return mapper.toResponse(job);
    if (job.getStatus() != RecordingTranslationStatus.PROCESSING
        || !job.getLeaseUntil().isAfter(Instant.now())) {
      throw conflict();
    }
    job.setErrorCode(request.code());
    job.setFailureHash(fingerprint);
    job.setLeaseUntil(null);
    if (request.retryable() && job.getAttempt() < job.getMaxAttempts()) {
      job.setStatus(RecordingTranslationStatus.RETRY_WAIT);
      job.setRetryAt(Instant.now().plusSeconds(properties.getRetryDelaySeconds()));
      outbox.save(new RecordingJobOutbox(jobId, job.getAttempt() + 1L, job.getRetryAt()));
    } else {
      job.setStatus(RecordingTranslationStatus.FAILED);
      job.setActiveSlot(null);
    }
    return mapper.toResponse(jobs.saveAndFlush(job));
  }

  private RecordingTranslationSource requireManager(String recordingId, Long userId) {
    var user = users.getUserById(userId);
    users.validateUserStatus(user);
    if (user.getAccountType() != AccountType.LECTURER) throw denied();
    return recordings.requireTranslationSource(recordingId, userId);
  }

  private RecordingTranslationJob requireAttempt(
      String jobId, RecordingWorkerIdentity worker, boolean active) {
    if (!"attempt".equals(worker.scope()) || !jobId.equals(worker.jobId())) throw denied();
    RecordingTranslationJob job = lock(jobId);
    if (!Objects.equals(job.getWorkerId(), worker.workerId())
        || !Objects.equals(job.getAttemptId(), worker.attemptId())
        || !job.getRecordingId().equals(worker.recordingId())
        || !job.getRunId().equals(worker.runId())
        || job.getRecordingEpoch() != worker.recordingEpoch()
        || job.getFencingToken() != worker.fencingToken()) {
      throw conflict();
    }
    if (active
        && (job.getStatus() != RecordingTranslationStatus.PROCESSING
            || job.getLeaseUntil() == null
            || !job.getLeaseUntil().isAfter(Instant.now()))) throw conflict();
    return job;
  }

  private void saveCheckpoint(RecordingTranslationJob job, CheckpointRequest checkpoint) {
    String prefix =
        "recording-translations/"
            + job.getRecordingId()
            + "/runs/"
            + job.getRunId()
            + "/checkpoints/";
    String key = checkpoint.objectKey();
    require(
        key != null
            && key.startsWith(prefix)
            && key.length() <= 1024
            && !key.contains("\\")
            && !key.matches(".*[\\p{Cntrl}].*")
            && !key.endsWith("/")
            && !key.contains("//")
            && java.util.Arrays.stream(key.split("/"))
                .noneMatch(part -> part.equals(".") || part.equals("..")));
    if (key.equals(job.getCheckpointKey())
        && !checkpoint.sha256().equals(job.getCheckpointSha256())) {
      throw conflict();
    }
    if (checkpoint.lastCompletedSample() < job.getCheckpointSample()
        || (job.getSourceSha256() != null
            && !job.getSourceSha256().equals(checkpoint.sourceSha256()))) {
      throw conflict();
    }
    job.setCheckpointKey(key);
    job.setCheckpointSha256(checkpoint.sha256());
    job.setCheckpointSample(checkpoint.lastCompletedSample());
    job.setSourceSha256(checkpoint.sourceSha256());
  }

  private RecordingWorkerGrantResponse grant(RecordingTranslationJob job) {
    CourseContext course;
    try {
      course = json.readValue(job.getCourseContextJson(), CourseContext.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid persisted recording course context", exception);
    }
    var attempt =
        new RecordingJobContextResponse.Attempt(
            job.getRecordingId(),
            job.getId(),
            job.getRunId(),
            job.getRecordingEpoch(),
            job.getAttemptId(),
            job.getFencingToken());
    var context =
        new RecordingJobContextResponse(
            1,
            "recording.job-context",
            attempt,
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
            course);
    var identity =
        new RecordingWorkerIdentity(
            job.getId(),
            job.getWorkerId(),
            "attempt",
            job.getRecordingId(),
            job.getRunId(),
            job.getRecordingEpoch(),
            job.getAttemptId(),
            job.getFencingToken());
    var checkpoint =
        job.getCheckpointKey() == null
            ? null
            : new CheckpointRequest(
                job.getCheckpointKey(),
                job.getCheckpointSha256(),
                job.getSourceSha256(),
                job.getCheckpointSample());
    return new RecordingWorkerGrantResponse(
        context,
        checkpoint,
        job.getLeaseUntil(),
        tokens.issue(identity),
        Instant.now().plusSeconds(properties.getTokenTtlSeconds()));
  }

  private CourseContext courseContext(RecordingTranslationSource source) {
    if (source.classSectionId() == null) return null;
    return subjects
        .findSubject(source.classSectionId())
        .map(
            subject ->
                new CourseContext(
                    subject.code(),
                    subject.name(),
                    properties
                        .getTerminologyProfiles()
                        .getOrDefault(subject.code(), properties.getTerminologyProfile()),
                    1))
        .orElse(null);
  }

  private RecordingTranslationJob findForRecording(String recordingId, String jobId) {
    return jobs.findById(jobId)
        .filter(job -> job.getRecordingId().equals(recordingId))
        .orElseThrow(this::notFound);
  }

  private RecordingTranslationJob lock(String jobId) {
    return jobs.findByIdForUpdate(jobId).orElseThrow(this::notFound);
  }

  private void requireEnabled() {
    if (!properties.isEnabled())
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_DISABLED);
  }

  private void requireConfiguration() {
    if (properties.getSettingsSha256() == null
        || !properties.getSettingsSha256().matches("[a-f0-9]{64}")
        || properties.getModelManifestSha256() == null
        || !properties.getModelManifestSha256().matches("[a-f0-9]{64}")
        || properties.getProfileId() == null
        || !properties.getProfileId().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
        || properties.getProfileVersion() < 1
        || properties.getMaxAttempts() < 1
        || properties.getMaxAttempts() > 10
        || properties.getLeaseSeconds() < 30
        || properties.getLeaseSeconds() > 600
        || properties.getTokenTtlSeconds() <= properties.getLeaseSeconds()
        || properties.getTokenTtlSeconds() > 3600
        || properties.getRetryDelaySeconds() < 1
        || properties.getRetryDelaySeconds() > 3600
        || properties.getWorkerSecret() == null
        || properties.getWorkerSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_CONFIGURATION);
    }
  }

  private String json(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot serialize recording contract", exception);
    }
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private void require(boolean condition) {
    if (!condition) throw BusinessException.invalidInput(AiErrorConstants.RECORDING_INVALID);
  }

  private BusinessException conflict() {
    return new BusinessException(StatusCode.CONFLICT, AiErrorConstants.RECORDING_CONFLICT);
  }

  private BusinessException denied() {
    return BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
  }

  private BusinessException notFound() {
    return BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND);
  }
}
