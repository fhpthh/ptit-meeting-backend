package org.ptit.meeting.modules.ai.service.impl;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus;
import org.ptit.meeting.modules.ai.dto.internal.RecordingDispatchSnapshot;
import org.ptit.meeting.modules.ai.entity.RecordingJobOutbox;
import org.ptit.meeting.modules.ai.repository.RecordingJobOutboxRepository;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationJobRepository;
import org.ptit.meeting.modules.ai.service.RecordingDispatchStateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingDispatchStateServiceImpl implements RecordingDispatchStateService {
  private final RecordingJobOutboxRepository outbox;
  private final RecordingTranslationJobRepository jobs;
  private final AiRecordingProperties properties;

  private boolean enabled() {
    var config = properties.getDispatch();
    if (config.getBatchSize() < 1
        || config.getBatchSize() > 100
        || config.getReconcileSeconds() < 1
        || config.getReconcileSeconds() > 3600)
      throw new IllegalStateException(
          "Invalid recording dispatch batch/reconciliation configuration");
    return properties.isEnabled() && config.isEnabled();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecordingDispatchSnapshot> due() {
    if (!enabled()) return List.of();
    Instant now = Instant.now();
    return outbox.due(
        now,
        now.minusSeconds(properties.getDispatch().getReconcileSeconds()),
        PageRequest.of(0, properties.getDispatch().getBatchSize()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> expired() {
    return enabled()
        ? jobs.expired(Instant.now(), PageRequest.of(0, properties.getDispatch().getBatchSize()))
        : List.of();
  }

  @Override
  @Transactional
  public void published(String eventId) {
    if (enabled()) outbox.published(eventId, Instant.now());
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public boolean recover(String jobId) {
    if (!enabled()) return false;
    // Job-only fencing: never acquire a recording lock after this lock.
    // No source access or new grant; claim retains full authorization checks.
    var job = jobs.findByIdForUpdate(jobId).orElse(null);
    Instant now = Instant.now();
    if (job == null
        || job.getStatus() != RecordingTranslationStatus.PROCESSING
        || job.getLeaseUntil() == null
        || job.getLeaseUntil().isAfter(now)) return false;
    job.setFencingToken(Math.addExact(job.getFencingToken(), 1));
    job.setLeaseUntil(null);
    job.setErrorCode("RECORDING_LEASE_EXPIRED");
    if (job.getAttempt() >= job.getMaxAttempts()) {
      job.setStatus(RecordingTranslationStatus.FAILED);
      job.setActiveSlot(null);
    } else {
      if (properties.getRetryDelaySeconds() < 1 || properties.getRetryDelaySeconds() > 3600)
        throw new IllegalStateException("Invalid retry delay");
      job.setStatus(RecordingTranslationStatus.RETRY_WAIT);
      job.setRetryAt(now.plusSeconds(properties.getRetryDelaySeconds()));
      outbox.save(new RecordingJobOutbox(jobId, job.getAttempt() + 1L, job.getRetryAt()));
    }
    jobs.saveAndFlush(job);
    return true;
  }
}
