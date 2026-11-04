package org.ptit.meeting.modules.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionScheduleSyncRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.ptit.meeting.modules.meeting.event.MeetingRuntimeSessionEndedEvent;
import org.ptit.meeting.modules.meeting.mapper.MeetingRuntimeSessionMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingRuntimeSessionServiceImpl implements MeetingRuntimeSessionService {

  private static final int TITLE_MAX_LENGTH = 150;
  private static final int SCHEDULE_OCCURRENCE_MAX_LENGTH = 100;

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingRuntimeSessionMapper runtimeSessionMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse create(
      String roomId,
      MeetingRuntimeSessionCreateRequest request,
      Long currentUserId
  ) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireRuntimeSessionHost(room.getId(), currentUserId);
    requireActiveRoom(room);

    String title = normalizeTitle(request == null ? null : request.title());
    Instant scheduledStartAt = request.scheduledStartAt();
    Instant scheduledEndAt = request.scheduledEndAt();
    validateScheduleTime(scheduledStartAt, scheduledEndAt);

    MeetingRuntimeSession runtimeSession = new MeetingRuntimeSession(
        room.getId(),
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
  @Transactional
  public MeetingRuntimeSessionResponse syncFromSchedule(
      MeetingRuntimeSessionScheduleSyncRequest request
  ) {
    if (request == null) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_SCHEDULE_INVALID
      );
    }

    MeetingRoom room = findRoom(request.meetingRoomId());
    authorizationService.requireRuntimeSessionHost(room.getId(), request.createdByUserId());
    requireActiveRoom(room);

    String occurrenceId = normalizeScheduleOccurrenceId(request.scheduleOccurrenceId());
    String title = normalizeTitle(request.title());
    validateScheduleTime(request.scheduledStartAt(), request.scheduledEndAt());

    Optional<MeetingRuntimeSession> existing = runtimeSessionRepository.findByScheduleOccurrenceId(
        occurrenceId
    );
    if (existing.isPresent()) {
      return synchronize(existing.get(), room.getId(), request, title);
    }

    MeetingRuntimeSession runtimeSession = new MeetingRuntimeSession(
        room.getId(),
        title,
        generateProviderRoomName(),
        occurrenceId,
        request.scheduledStartAt(),
        request.scheduledEndAt(),
        request.createdByUserId()
    );
    return runtimeSessionMapper.toResponse(saveScheduleSync(runtimeSession));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingRuntimeSessionResponse> getByRoom(String roomId, Long currentUserId) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireActiveMember(room.getId(), currentUserId);
    return runtimeSessionRepository.findAllByMeetingRoomIdOrderByScheduledStartAtAsc(room.getId())
        .stream()
        .map(runtimeSessionMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public MeetingRuntimeSessionResponse getById(String runtimeSessionId, Long currentUserId) {
    MeetingRuntimeSession runtimeSession = findRuntimeSession(runtimeSessionId);
    authorizationService.requireActiveMember(runtimeSession.getMeetingRoomId(), currentUserId);
    return runtimeSessionMapper.toResponse(runtimeSession);
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse start(String runtimeSessionId, Long currentUserId) {
    MeetingRuntimeSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    MeetingRoom room = findRoom(runtimeSession.getMeetingRoomId());
    requireActiveRoom(room);
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.SCHEDULED);

    runtimeSession.setStatus(MeetingRuntimeSessionStatus.ACTIVE);
    runtimeSession.setActualStartAt(Instant.now());
    return runtimeSessionMapper.toResponse(saveTransition(runtimeSession));
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse end(String runtimeSessionId, Long currentUserId) {
    MeetingRuntimeSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.ACTIVE);

    runtimeSession.setStatus(MeetingRuntimeSessionStatus.ENDED);
    runtimeSession.setActualEndAt(Instant.now());
    MeetingRuntimeSession endedRuntimeSession = saveTransition(runtimeSession);
    eventPublisher.publishEvent(new MeetingRuntimeSessionEndedEvent(
        endedRuntimeSession.getId(),
        endedRuntimeSession.getProviderRoomName()
    ));
    return runtimeSessionMapper.toResponse(endedRuntimeSession);
  }

  @Override
  @Transactional
  public MeetingRuntimeSessionResponse cancel(String runtimeSessionId, Long currentUserId) {
    MeetingRuntimeSession runtimeSession = findManagedRuntimeSession(
        runtimeSessionId,
        currentUserId
    );
    requireStatus(runtimeSession, MeetingRuntimeSessionStatus.SCHEDULED);

    runtimeSession.setStatus(MeetingRuntimeSessionStatus.CANCELLED);
    return runtimeSessionMapper.toResponse(saveTransition(runtimeSession));
  }

  private MeetingRuntimeSessionResponse synchronize(
      MeetingRuntimeSession runtimeSession,
      String roomId,
      MeetingRuntimeSessionScheduleSyncRequest request,
      String title
  ) {
    if (!runtimeSession.getMeetingRoomId().equals(roomId)) {
      throw BusinessException.existed(
          MeetingErrorConstants.RUNTIME_SESSION_SCHEDULE_CONFLICT,
          request.scheduleOccurrenceId()
      );
    }
    if (runtimeSession.getStatus() != MeetingRuntimeSessionStatus.SCHEDULED) {
      return runtimeSessionMapper.toResponse(runtimeSession);
    }

    runtimeSession.setTitle(title);
    runtimeSession.setScheduledStartAt(request.scheduledStartAt());
    runtimeSession.setScheduledEndAt(request.scheduledEndAt());
    return runtimeSessionMapper.toResponse(saveScheduleSync(runtimeSession));
  }

  private MeetingRuntimeSession saveScheduleSync(MeetingRuntimeSession runtimeSession) {
    try {
      return runtimeSessionRepository.saveAndFlush(runtimeSession);
    } catch (DataIntegrityViolationException exception) {
      throw BusinessException.existed(
          MeetingErrorConstants.RUNTIME_SESSION_SCHEDULE_CONFLICT,
          runtimeSession.getScheduleOccurrenceId()
      );
    } catch (OptimisticLockingFailureException exception) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_CONCURRENT_UPDATE
      );
    }
  }

  private MeetingRuntimeSession saveTransition(MeetingRuntimeSession runtimeSession) {
    try {
      return runtimeSessionRepository.saveAndFlush(runtimeSession);
    } catch (OptimisticLockingFailureException exception) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_CONCURRENT_UPDATE,
          runtimeSession.getId()
      );
    }
  }

  private MeetingRuntimeSession findManagedRuntimeSession(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingRuntimeSession runtimeSession = findRuntimeSession(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    return runtimeSession;
  }

  private MeetingRuntimeSession findRuntimeSession(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND);
    }
    return runtimeSessionRepository.findById(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND,
            runtimeSessionId
        ));
  }

  private MeetingRoom findRoom(String roomId) {
    if (roomId == null || roomId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.ROOM_NOT_FOUND);
    }
    return roomRepository.findById(roomId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.ROOM_NOT_FOUND,
            roomId
        ));
  }

  private void requireActiveRoom(MeetingRoom room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getId());
    }
  }

  private void requireStatus(
      MeetingRuntimeSession runtimeSession,
      MeetingRuntimeSessionStatus expectedStatus
  ) {
    if (runtimeSession.getStatus() != expectedStatus) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_STATE_INVALID,
          runtimeSession.getStatus(),
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

  private String normalizeScheduleOccurrenceId(String occurrenceId) {
    if (occurrenceId == null
        || occurrenceId.isBlank()
        || occurrenceId.trim().length() > SCHEDULE_OCCURRENCE_MAX_LENGTH) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_SCHEDULE_INVALID
      );
    }
    return occurrenceId.trim();
  }

  private void validateScheduleTime(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RUNTIME_SESSION_TIME_INVALID);
    }
  }

  private String generateProviderRoomName() {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    return "runtime-session-" + suffix.toLowerCase(Locale.ROOT);
  }
}
