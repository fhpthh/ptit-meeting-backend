package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.RecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.integration.storage.MeetingRecordingStorage;
import org.ptit.meeting.integration.storage.MeetingRecordingStorage.StorageException;
import org.ptit.meeting.integration.storage.MeetingRecordingStorage.StoredObject;
import org.ptit.meeting.integration.storage.MeetingRecordingStorage.UploadPartGrant;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRecordingStatus;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCompleteRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingUploadPartResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRecording;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.mapper.MeetingRecordingMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRecordingRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRecordingServiceImpl implements MeetingRecordingService {

  private static final String RECORDING_TOPIC = "meeting.recording.v1";
  private static final String RECORDING_KEY_PREFIX = "recordings";
  private static final int MIN_PART_NUMBER = 1;
  private static final int MAX_PART_NUMBER = 10_000;
  private static final Set<MeetingRecordingStatus> UPLOAD_STATUSES = Set.of(
      MeetingRecordingStatus.ACTIVE,
      MeetingRecordingStatus.FINALIZING
  );

  private final MeetingRecordingRepository recordingRepository;
  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingModerationService moderationService;
  private final MeetingRecordingMapper recordingMapper;
  private final LiveKitGateway liveKitGateway;
  private final RecordingProperties recordingProperties;
  private final ObjectProvider<MeetingRecordingStorage> storageProvider;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  @Override
  public MeetingRecordingResponse start(
      String runtimeSessionId,
      MeetingRecordingCreateRequest request,
      Long currentUserId
  ) {
    MeetingRecordingStorage storage = requireStorage();
    PreparedStart prepared = requireTransactionResult(transactionTemplate.execute(
        status -> prepareStart(runtimeSessionId, currentUserId)
    ));
    String uploadId;
    try {
      uploadId = storage.createMultipartUpload(prepared.objectKey(), request.mimeType());
    } catch (StorageException exception) {
      throw recordingUnavailable();
    }

    try {
      MeetingRecordingResponse response = requireTransactionResult(transactionTemplate.execute(
          status -> persistStartedRecording(prepared, request.mimeType(), uploadId, currentUserId)
      ));
      broadcast(prepared.providerRoomName(), response);
      return response;
    } catch (RuntimeException exception) {
      abortStorageQuietly(storage, prepared.objectKey(), uploadId, prepared.recordingId());
      if (isActiveSlotConflict(exception)) {
        throw BusinessException.existed(MeetingErrorConstants.RECORDING_ALREADY_ACTIVE);
      }
      if (exception instanceof DataIntegrityViolationException) {
        throw recordingUnavailable();
      }
      throw exception;
    }
  }

  @Override
  public MeetingRecordingUploadPartResponse createUploadPartUrl(
      String recordingId,
      int partNumber,
      Long currentUserId
  ) {
    if (partNumber < MIN_PART_NUMBER || partNumber > MAX_PART_NUMBER) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RECORDING_PART_NUMBER_INVALID
      );
    }
    UploadTarget target = requireTransactionResult(transactionTemplate.execute(status -> {
      MeetingRecording recording = findRecordingForUpdate(recordingId);
      requireUploadOwner(recording, currentUserId);
      if (recording.getStatus() != MeetingRecordingStatus.ACTIVE) {
        throw BusinessException.invalidInput(MeetingErrorConstants.RECORDING_NOT_ACTIVE);
      }
      recording.touchUpload();
      recordingRepository.save(recording);
      return toUploadTarget(recording);
    }));

    try {
      UploadPartGrant grant = requireStorage().createUploadPartGrant(
          target.objectKey(),
          target.uploadId(),
          partNumber
      );
      return new MeetingRecordingUploadPartResponse(
          partNumber,
          grant.url(),
          grant.expiresAt()
      );
    } catch (StorageException exception) {
      log.error(
          "Unable to authorize browser recording part: recordingId={}, partNumber={}",
          recordingId,
          partNumber,
          exception
      );
      throw recordingUnavailable();
    }
  }

  @Override
  public MeetingRecordingResponse complete(
      String recordingId,
      MeetingRecordingCompleteRequest request,
      Long currentUserId
  ) {
    CompletionTarget target = requireTransactionResult(transactionTemplate.execute(status -> {
      MeetingRecording recording = findRecordingForUpdate(recordingId);
      requireUploadOwner(recording, currentUserId);
      if (recording.getStatus() == MeetingRecordingStatus.COMPLETED) {
        return new CompletionTarget(null, null, toResponse(recording));
      }
      if (recording.getStatus() != MeetingRecordingStatus.ACTIVE) {
        throw BusinessException.invalidInput(MeetingErrorConstants.RECORDING_NOT_ACTIVE);
      }
      recording.beginFinalizing();
      recordingRepository.save(recording);
      return new CompletionTarget(
          recording.getObjectKey(),
          recording.getStorageUploadId(),
          null
      );
    }));
    if (target.completedResponse() != null) {
      return target.completedResponse();
    }

    StoredObject storedObject;
    try {
      storedObject = requireStorage().completeMultipartUpload(
          target.objectKey(),
          target.uploadId()
      );
    } catch (StorageException exception) {
      transactionTemplate.executeWithoutResult(status -> recordingRepository
          .findByIdForUpdate(recordingId)
          .filter(recording -> recording.getStatus() == MeetingRecordingStatus.FINALIZING)
          .ifPresent(recording -> {
            recording.resumeUpload();
            recordingRepository.save(recording);
          }));
      log.error("Unable to complete browser recording upload: recordingId={}", recordingId, exception);
      throw recordingUnavailable();
    }

    MeetingRecordingResponse response = requireTransactionResult(transactionTemplate.execute(
        status -> {
          MeetingRecording recording = findRecordingForUpdate(recordingId);
          if (recording.getStatus() != MeetingRecordingStatus.FINALIZING) {
            throw BusinessException.invalidInput(MeetingErrorConstants.RECORDING_NOT_ACTIVE);
          }
          recording.complete(storedObject.sizeBytes(), request.durationMs());
          recordingRepository.save(recording);
          return toResponse(recording);
        }
    ));
    broadcast(response);
    return response;
  }

  @Override
  public MeetingRecordingResponse abort(String recordingId, Long currentUserId) {
    AbortResult result = requireTransactionResult(transactionTemplate.execute(status -> {
      MeetingRecording recording = findRecordingForUpdate(recordingId);
      requireUploadOwner(recording, currentUserId);
      if (recording.getStatus().isTerminal()) {
        return new AbortResult(null, toResponse(recording));
      }
      UploadTarget target = toUploadTarget(recording);
      recording.abort(null);
      recordingRepository.save(recording);
      return new AbortResult(target, toResponse(recording));
    }));
    if (result.uploadTarget() != null) {
      abortStorageQuietly(
          requireStorage(),
          result.uploadTarget().objectKey(),
          result.uploadTarget().uploadId(),
          recordingId
      );
      broadcast(result.response());
    }
    return result.response();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingRecordingResponse> getByRuntimeSession(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = findRuntimeSession(runtimeSessionId);
    requireViewAccess(runtimeSession.getPublicId(), runtimeSession.getMeetingRoomId(), currentUserId);
    return recordingRepository.findAllByRuntimeSessionIdOrderByCreatedAtDesc(runtimeSessionId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public MeetingRecordingResponse getById(String recordingId, Long currentUserId) {
    MeetingRecording recording = findRecording(recordingId);
    requireViewAccess(
        recording.getRuntimeSessionId(),
        recording.getMeetingRoomId(),
        currentUserId
    );
    return toResponse(recording);
  }

  @Override
  public void cleanupStaleUploads() {
    if (!recordingProperties.isEnabled()) {
      return;
    }
    Instant cutoff = Instant.now().minusMillis(
        Math.max(60_000, recordingProperties.getStaleUploadTimeoutMs())
    );
    List<String> recordingIds = recordingRepository
        .findTop50ByStatusInAndLastActivityAtBeforeOrderByLastActivityAtAsc(
            UPLOAD_STATUSES,
            cutoff
        )
        .stream()
        .map(MeetingRecording::getId)
        .toList();
    for (String recordingId : recordingIds) {
      cleanupStaleUpload(recordingId, cutoff);
    }
  }

  private PreparedStart prepareStart(String runtimeSessionId, Long currentUserId) {
    requireStorageConfiguration();
    MeetingSession runtimeSession = findRuntimeSession(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    moderationService.requireNotBanned(runtimeSession.getPublicId(), currentUserId);
    if (runtimeSession.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSessionId
      );
    }
    if (recordingRepository.findByActiveSlot(runtimeSessionId).isPresent()) {
      throw BusinessException.existed(MeetingErrorConstants.RECORDING_ALREADY_ACTIVE);
    }

    String recordingId = UUID.randomUUID().toString();
    return new PreparedStart(
        recordingId,
        runtimeSession.getPublicId(),
        runtimeSession.getMeetingRoomId(),
        runtimeSession.getProviderRoomName(),
        buildObjectKey(
            runtimeSession.getMeetingRoomId(),
            runtimeSession.getPublicId(),
            recordingId
        )
    );
  }

  private MeetingRecordingResponse persistStartedRecording(
      PreparedStart prepared,
      String mimeType,
      String uploadId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = findRuntimeSession(prepared.runtimeSessionId());
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    if (runtimeSession.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSession.getPublicId()
      );
    }
    if (recordingRepository.findByActiveSlot(runtimeSession.getPublicId()).isPresent()) {
      throw BusinessException.existed(MeetingErrorConstants.RECORDING_ALREADY_ACTIVE);
    }
    MeetingRecording recording = new MeetingRecording(
        prepared.runtimeSessionId(),
        prepared.meetingRoomId(),
        prepared.objectKey(),
        mimeType.trim(),
        uploadId,
        currentUserId
    );
    recording.setId(prepared.recordingId());
    recordingRepository.saveAndFlush(recording);
    return toResponse(recording);
  }

  private void cleanupStaleUpload(String recordingId, Instant cutoff) {
    AbortResult result = transactionTemplate.execute(status -> recordingRepository
        .findByIdForUpdate(recordingId)
        .filter(recording -> UPLOAD_STATUSES.contains(recording.getStatus()))
        .filter(recording -> recording.getLastActivityAt().isBefore(cutoff))
        .map(recording -> {
          UploadTarget uploadTarget = toUploadTarget(recording);
          recording.fail("Browser recording upload timed out");
          recordingRepository.save(recording);
          return new AbortResult(uploadTarget, toResponse(recording));
        })
        .orElse(null));
    if (result == null) {
      return;
    }
    abortStorageQuietly(
        requireStorage(),
        result.uploadTarget().objectKey(),
        result.uploadTarget().uploadId(),
        recordingId
    );
    log.warn("Cleaned up stale browser recording upload: recordingId={}", recordingId);
    broadcast(result.response());
  }

  private void requireUploadOwner(MeetingRecording recording, Long currentUserId) {
    authorizationService.requireRuntimeSessionHost(
        recording.getMeetingRoomId(),
        currentUserId
    );
    moderationService.requireNotBanned(recording.getRuntimeSessionId(), currentUserId);
    if (!recording.getRequestedByUserId().equals(currentUserId)) {
      throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
    }
  }

  private void requireViewAccess(
      String runtimeSessionId,
      String meetingRoomId,
      Long currentUserId
  ) {
    authorizationService.requireActiveMember(meetingRoomId, currentUserId);
    moderationService.requireNotBanned(runtimeSessionId, currentUserId);
  }

  private MeetingSession findRuntimeSession(String runtimeSessionId) {
    if (!hasText(runtimeSessionId)) {
      throw BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND);
    }
    return runtimeSessionRepository.findByPublicId(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND,
            runtimeSessionId
        ));
  }

  private MeetingRecording findRecording(String recordingId) {
    if (!hasText(recordingId)) {
      throw BusinessException.notExisted(MeetingErrorConstants.RECORDING_NOT_FOUND);
    }
    return recordingRepository.findById(recordingId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RECORDING_NOT_FOUND,
            recordingId
        ));
  }

  private MeetingRecording findRecordingForUpdate(String recordingId) {
    if (!hasText(recordingId)) {
      throw BusinessException.notExisted(MeetingErrorConstants.RECORDING_NOT_FOUND);
    }
    return recordingRepository.findByIdForUpdate(recordingId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RECORDING_NOT_FOUND,
            recordingId
        ));
  }

  private UploadTarget toUploadTarget(MeetingRecording recording) {
    if (!hasText(recording.getStorageUploadId())) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RECORDING_NOT_ACTIVE);
    }
    return new UploadTarget(recording.getObjectKey(), recording.getStorageUploadId());
  }

  private MeetingRecordingStorage requireStorage() {
    requireStorageConfiguration();
    MeetingRecordingStorage storage = storageProvider.getIfAvailable();
    if (storage == null) {
      throw recordingUnavailable();
    }
    return storage;
  }

  private RecordingProperties.Storage requireStorageConfiguration() {
    if (!recordingProperties.isEnabled()) {
      throw recordingUnavailable();
    }
    RecordingProperties.Storage storage = recordingProperties.getStorage();
    if (storage == null
        || !hasText(storage.getBucket())
        || !hasText(storage.getPublicBaseUrl())) {
      throw recordingUnavailable();
    }
    try {
      URI publicBaseUri = URI.create(storage.getPublicBaseUrl().trim());
      if (!("http".equalsIgnoreCase(publicBaseUri.getScheme())
          || "https".equalsIgnoreCase(publicBaseUri.getScheme()))
          || publicBaseUri.getHost() == null
          || publicBaseUri.getUserInfo() != null
          || publicBaseUri.getQuery() != null
          || publicBaseUri.getFragment() != null) {
        throw recordingUnavailable();
      }
    } catch (IllegalArgumentException exception) {
      throw recordingUnavailable();
    }
    return storage;
  }

  private MeetingRecordingResponse toResponse(MeetingRecording recording) {
    String publicUrl = recording.getStatus() == MeetingRecordingStatus.COMPLETED
        ? buildPublicUrl(recording.getObjectKey())
        : null;
    return recordingMapper.toResponse(recording, publicUrl);
  }

  private String buildPublicUrl(String objectKey) {
    RecordingProperties.Storage storage = requireStorageConfiguration();
    UriComponentsBuilder builder = UriComponentsBuilder
        .fromUriString(storage.getPublicBaseUrl().trim())
        .pathSegment(storage.getBucket().trim());
    for (String segment : objectKey.split("/")) {
      if (!segment.isBlank()) {
        builder.pathSegment(segment);
      }
    }
    return builder.build().encode().toUriString();
  }

  private String buildObjectKey(
      String meetingRoomId,
      String runtimeSessionId,
      String recordingId
  ) {
    return String.join(
        "/",
        RECORDING_KEY_PREFIX,
        meetingRoomId,
        runtimeSessionId,
        recordingId + ".webm"
    );
  }

  private void broadcast(MeetingRecordingResponse response) {
    runtimeSessionRepository.findByPublicId(response.runtimeSessionId())
        .ifPresent(session -> broadcast(session.getProviderRoomName(), response));
  }

  private void broadcast(String providerRoomName, MeetingRecordingResponse response) {
    try {
      liveKitGateway.broadcastReliableData(
          providerRoomName,
          objectMapper.writeValueAsBytes(new RecordingStateEvent(
              response.id(),
              response.runtimeSessionId(),
              response.status(),
              response.startedAt(),
              response.endedAt()
          )),
          RECORDING_TOPIC
      );
    } catch (JsonProcessingException | LiveKitIntegrationException exception) {
      log.warn(
          "Unable to broadcast meeting recording state: recordingId={}",
          response.id()
      );
    }
  }

  private void abortStorageQuietly(
      MeetingRecordingStorage storage,
      String objectKey,
      String uploadId,
      String recordingId
  ) {
    try {
      storage.abortMultipartUpload(objectKey, uploadId);
    } catch (StorageException exception) {
      log.warn("Unable to abort recording multipart upload: recordingId={}", recordingId);
    }
  }

  private BusinessException recordingUnavailable() {
    return new BusinessException(
        StatusCode.ERROR_IN_BACKEND,
        MeetingErrorConstants.RECORDING_UNAVAILABLE
    );
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean isActiveSlotConflict(RuntimeException exception) {
    if (!(exception instanceof DataIntegrityViolationException integrityException)) {
      return false;
    }
    Throwable cause = integrityException.getMostSpecificCause();
    String message = cause.getMessage();
    return message != null && message.contains("uk_meeting_recordings_active_slot");
  }

  private <T> T requireTransactionResult(T result) {
    if (result == null) {
      throw new IllegalStateException("Recording transaction returned no result");
    }
    return result;
  }

  private record PreparedStart(
      String recordingId,
      String runtimeSessionId,
      String meetingRoomId,
      String providerRoomName,
      String objectKey
  ) {
  }

  private record UploadTarget(String objectKey, String uploadId) {
  }

  private record CompletionTarget(
      String objectKey,
      String uploadId,
      MeetingRecordingResponse completedResponse
  ) {
  }

  private record AbortResult(
      UploadTarget uploadTarget,
      MeetingRecordingResponse response
  ) {
  }

  private record RecordingStateEvent(
      String recordingId,
      String runtimeSessionId,
      MeetingRecordingStatus status,
      Instant startedAt,
      Instant endedAt
  ) {
  }
}
