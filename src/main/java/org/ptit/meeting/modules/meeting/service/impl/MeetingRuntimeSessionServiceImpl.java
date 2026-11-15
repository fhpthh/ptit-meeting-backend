package org.ptit.meeting.modules.meeting.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.event.MeetingRuntimeSessionEndedEvent;
import org.ptit.meeting.modules.meeting.mapper.MeetingRuntimeSessionMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingChatService;
import org.ptit.meeting.modules.meeting.service.MeetingHandRaiseService;
import org.ptit.meeting.modules.meeting.service.MeetingHostPresenceService;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRuntimeSessionServiceImpl implements MeetingRuntimeSessionService {

  private static final int TITLE_MAX_LENGTH = 150;
  private static final Duration STUDENT_OVERTIME_LIMIT = Duration.ofHours(1);

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingRuntimeSessionMapper runtimeSessionMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final LiveKitGateway liveKitGateway;
  private final MeetingChatService chatService;
  private final MeetingHandRaiseService handRaiseService;
  private final MeetingHostPresenceService hostPresenceService;
  private final PlatformTransactionManager transactionManager;

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse create(
      String roomId,
      MeetingRuntimeSessionCreateRequest request,
      Long currentUserId
  ) {
    Room room = findRoom(roomId);
    authorizationService.requireRuntimeSessionHost(room.getPublicId(), currentUserId);
    requireActiveRoom(room);

    String title = normalizeTitle(request == null ? null : request.title());
    Instant scheduledStartAt = request.scheduledStartAt();
    Instant scheduledEndAt = request.scheduledEndAt();
    validateScheduleTime(scheduledStartAt, scheduledEndAt);

    MeetingSession runtimeSession = new MeetingSession(
        room,
        title,
        generateProviderRoomName(),
        null,
        scheduledStartAt,
        scheduledEndAt,
        currentUserId
    );
    return runtimeSessionMapper.toResponse(runtimeSessionRepository.saveAndFlush(runtimeSession));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingRuntimeSessionResponse> getByRoom(String roomId, Long currentUserId) {
    Room room = findRoom(roomId);
    authorizationService.requireActiveMember(room.getPublicId(), currentUserId);
    return runtimeSessionRepository.findAllByRoom_PublicIdOrderByScheduledStartAsc(room.getPublicId())
        .stream()
        .map(runtimeSessionMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public MeetingRuntimeSessionResponse getById(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findRuntimeSession(runtimeSessionId);
    authorizationService.requireActiveMember(runtimeSession.getMeetingRoomId(), currentUserId);
    return runtimeSessionMapper.toResponse(runtimeSession);
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse start(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    Room room = findRoom(runtimeSession.getMeetingRoomId());
    requireActiveRoom(room);
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.SCHEDULED);

    Instant now = Instant.now();
    if (!runtimeSession.getScheduledEndAt().isAfter(now)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RUNTIME_SESSION_EXPIRED);
    }

    runtimeSession.setRuntimeStatus(MeetingRuntimeSessionStatus.ACTIVE);
    runtimeSession.setActualStartAt(now);
    return runtimeSessionMapper.toResponse(saveTransition(runtimeSession));
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse end(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    if (runtimeSession.getRuntimeStatus() == MeetingRuntimeSessionStatus.ENDED) {
      if (runtimeSession.isCleanupPending()) {
        eventPublisher.publishEvent(new MeetingRuntimeSessionEndedEvent(runtimeSessionId));
      }
      return runtimeSessionMapper.toResponse(runtimeSession);
    }
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.ACTIVE);
    finish(runtimeSession, Instant.now());
    return runtimeSessionMapper.toResponse(runtimeSession);
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse cancel(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.SCHEDULED);

    runtimeSession.setRuntimeStatus(MeetingRuntimeSessionStatus.CANCELLED);
    return runtimeSessionMapper.toResponse(saveTransition(runtimeSession));
  }

  @Override
  public void reconcileSessions() {
    Instant now = Instant.now();
    List<String> expiredIds = runtimeSessionRepository.findExpiredSessionIds(
        SessionStatus.SCHEDULED,
        LocalDateTime.ofInstant(now, ZoneId.of("Asia/Ho_Chi_Minh"))
    );
    for (String sessionId : expiredIds) {
      try {
        newTransaction().executeWithoutResult(status -> {
          MeetingSession session = findRuntimeSessionForUpdate(sessionId);
          if (session.getRuntimeStatus() == MeetingRuntimeSessionStatus.SCHEDULED
              && !session.getScheduledEndAt().isAfter(now)) {
            session.setRuntimeStatus(MeetingRuntimeSessionStatus.CANCELLED);
            saveTransition(session);
          }
        });
      } catch (RuntimeException exception) {
        log.error("Unable to expire meeting session: runtimeSessionId={}", sessionId, exception);
      }
    }
    for (String sessionId : runtimeSessionRepository.findPendingCleanupIds()) {
      try {
        cleanupEndedSession(sessionId);
      } catch (RuntimeException exception) {
        log.warn("Meeting cleanup will retry: runtimeSessionId={}", sessionId, exception);
      }
    }
  }

  @Override
  public void reconcileOvertime(
      String runtimeSessionId,
      Long observedVersion,
      String observedHostState,
      List<LiveKitParticipantSnapshot> participants
  ) {
    Set<String> identities = participants.stream()
        .map(LiveKitParticipantSnapshot::identity).collect(Collectors.toSet());
    // LiveKit was queried before this transaction; do not hold a row lock during that call.
    newTransaction().executeWithoutResult(status -> {
      MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
      Instant now = Instant.now();
      if (session.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE
          || session.getScheduledEndAt().isAfter(now)
          || !Objects.equals(session.getVersion(), observedVersion)) {
        return;
      }
      boolean hostPresent = authorizationService.getRuntimeSessionHostIds(session.getMeetingRoomId())
          .stream().anyMatch(userId -> identities.contains("user-" + userId));
      if (hostPresent) {
        hostPresenceService.markPresent(runtimeSessionId);
        return;
      }
      boolean gracePassed = hostPresenceService.hasReconnectGracePassed(
          runtimeSessionId, observedHostState, now
      );
      boolean overtimePassed = !now.isBefore(session.getScheduledEndAt().plus(STUDENT_OVERTIME_LIMIT));
      if (gracePassed && (participants.isEmpty() || overtimePassed)) {
        finish(session, now);
      }
    });
  }

  @Override
  public void cleanupEndedSession(String runtimeSessionId) {
    // A separate transaction also works from an AFTER_COMMIT listener.
    // Keep cleanup serialized with late admissions so a new retry cannot be cleared by an old cleanup.
    newTransaction().executeWithoutResult(status -> {
      MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
      if (session.getRuntimeStatus() != MeetingRuntimeSessionStatus.ENDED
          || !session.isCleanupPending()) {
        return;
      }
      // Stop media first; a Redis failure must not leave the call running.
      liveKitGateway.deleteRoom(session.getProviderRoomName());
      chatService.clearHistory(runtimeSessionId);
      handRaiseService.clear(runtimeSessionId);
      hostPresenceService.clear(runtimeSessionId);
      session.setCleanupPending(false);
      runtimeSessionRepository.saveAndFlush(session);
    });
  }

  private void finish(MeetingSession session, Instant endedAt) {
    session.setRuntimeStatus(MeetingRuntimeSessionStatus.ENDED);
    session.setActualEndAt(endedAt);
    session.setCleanupPending(true);
    saveTransition(session);
    eventPublisher.publishEvent(new MeetingRuntimeSessionEndedEvent(session.getPublicId()));
  }

  private TransactionTemplate newTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transaction;
  }

  private MeetingSession saveTransition(MeetingSession runtimeSession) {
    try {
      return runtimeSessionRepository.saveAndFlush(runtimeSession);
    } catch (OptimisticLockingFailureException exception) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_CONCURRENT_UPDATE,
          runtimeSession.getPublicId()
      );
    }
  }

  private MeetingSession findManagedRuntimeSession(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = findRuntimeSessionForUpdate(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    return runtimeSession;
  }

  private MeetingSession findRuntimeSessionForUpdate(String runtimeSessionId) {
    return runtimeSessionRepository.findByPublicIdForUpdate(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND, runtimeSessionId
        ));
  }

  private MeetingSession findRuntimeSession(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND);
    }
    return runtimeSessionRepository.findByPublicId(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND,
            runtimeSessionId
        ));
  }

  private Room findRoom(String roomId) {
    if (roomId == null || roomId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.ROOM_NOT_FOUND);
    }
    return roomRepository.findByPublicId(roomId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.ROOM_NOT_FOUND,
            roomId
        ));
  }

  private void requireActiveRoom(Room room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getPublicId());
    }
  }

  private void requireStatus(
      MeetingSession runtimeSession,
      MeetingRuntimeSessionStatus expectedStatus
  ) {
    if (runtimeSession.getRuntimeStatus() != expectedStatus) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_STATE_INVALID,
          runtimeSession.getRuntimeStatus(),
          expectedStatus
      );
    }
  }

  private String normalizeTitle(String title) {
    if (title == null || title.isBlank() || title.trim().length() > TITLE_MAX_LENGTH) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RUNTIME_SESSION_TITLE_INVALID);
    }
    return title.trim();
  }

  private void validateScheduleTime(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RUNTIME_SESSION_TIME_INVALID);
    }
  }

  private String generateProviderRoomName() {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    return "runtime-session-" + suffix;
  }
}
