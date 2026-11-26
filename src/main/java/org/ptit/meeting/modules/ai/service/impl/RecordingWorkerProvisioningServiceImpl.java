package org.ptit.meeting.modules.ai.service.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;
import org.ptit.meeting.modules.ai.dto.internal.RecordingTrustedWorker;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingProvisionRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ClaimRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;
import org.ptit.meeting.modules.ai.repository.RecordingJobOutboxRepository;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationJobRepository;
import org.ptit.meeting.modules.ai.service.RecordingTranslationJobService;
import org.ptit.meeting.modules.ai.service.RecordingWorkerProvisioningService;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingWorkerProvisioningServiceImpl implements RecordingWorkerProvisioningService {
  private final AiRecordingProperties properties;
  private final RecordingTranslationJobRepository jobs;
  private final RecordingJobOutboxRepository outbox;
  private final RecordingTranslationJobService claims;
  private final UserService users;
  private final MeetingRecordingService recordings;

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RecordingWorkerGrantResponse claim(
      RecordingTrustedWorker worker, RecordingProvisionRequest request) {
    if (!properties.isEnabled() || !properties.getProvisioning().isEnabled())
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_DISABLED);
    if (worker == null
        || !java.util.Objects.equals(properties.getProvisioning().getWorkerId(), worker.workerId()))
      throw BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
    var authority =
        jobs.findCompletionAuthority(request.jobId())
            .orElseThrow(
                () -> BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND));
    var user = users.getUserById(authority.requestedByUserId());
    users.validateUserStatus(user);
    if (user.getAccountType() != AccountType.LECTURER)
      throw BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
    recordings.requireTranslationSource(authority.recordingId(), authority.requestedByUserId());
    var job = jobs.findByIdForUpdate(request.jobId()).orElseThrow(this::conflict);
    if (!java.util.Objects.equals(job.getRecordingId(), authority.recordingId())
        || !java.util.Objects.equals(job.getRequestedByUserId(), authority.requestedByUserId()))
      throw conflict();
    var event = outbox.findById(request.eventId()).orElseThrow(this::conflict);
    Instant now = Instant.now();
    if (!event.getJobId().equals(job.getId())
        || request.dispatchVersion() == null
        || event.getDispatchVersion() != request.dispatchVersion()
        || event.getAvailableAt().isAfter(now)
        || (job.getRetryAt() != null && job.getRetryAt().isAfter(now))) throw conflict();
    boolean pending =
        (job.getStatus() == RecordingTranslationStatus.QUEUED
                || job.getStatus() == RecordingTranslationStatus.RETRY_WAIT)
            && event.getDispatchVersion() == job.getAttempt() + 1L;
    boolean duplicate =
        job.getStatus() == RecordingTranslationStatus.PROCESSING
            && job.getLeaseUntil() != null
            && job.getLeaseUntil().isAfter(now)
            && worker.workerId().equals(job.getWorkerId())
            && request.eventId().equals(job.getClaimRequestId())
            && event.getDispatchVersion() == job.getAttempt();
    if (!pending && !duplicate) throw conflict();
    return claims.claim(
        job.getId(),
        new RecordingWorkerIdentity(
            job.getId(), worker.workerId(), "claim", null, null, 0, null, 0),
        new ClaimRequest(event.getId()));
  }

  private BusinessException conflict() {
    return new BusinessException(StatusCode.CONFLICT, AiErrorConstants.RECORDING_CONFLICT);
  }
}
