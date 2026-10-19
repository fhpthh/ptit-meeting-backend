package org.ptit.meeting.modules.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionScheduleSyncRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.event.MeetingSessionEndedEvent;
import org.ptit.meeting.modules.meeting.mapper.MeetingSessionMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingSessionService;
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
public class MeetingSessionServiceImpl implements MeetingSessionService {

  private static final int TITLE_MAX_LENGTH = 150;
  private static final int SCHEDULE_OCCURRENCE_MAX_LENGTH = 100;

  private final MeetingSessionRepository sessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingSessionMapper sessionMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public MeetingSessionResponse create(
      String roomId,
      MeetingSessionCreateRequest request,
      Long currentUserId
  ) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireSessionHost(room.getId(), currentUserId);
    requireActiveRoom(room);

    String title = normalizeTitle(request == null ? null : request.title());
    Instant scheduledStartAt = request.scheduledStartAt();
    Instant scheduledEndAt = request.scheduledEndAt();
    validateScheduleTime(scheduledStartAt, scheduledEndAt);

    MeetingSession session = new MeetingSession(
        room.getId(),
        title,
        generateProviderRoomName(),
        null,
        scheduledStartAt,
        scheduledEndAt,
        currentUserId
    );
    return sessionMapper.toResponse(sessionRepository.saveAndFlush(session));
  }

  @Override
  @Transactional
  public MeetingSessionResponse syncFromSchedule(MeetingSessionScheduleSyncRequest request) {
    if (request == null) {
      throw BusinessException.invalidInput(MeetingErrorConstants.SESSION_SCHEDULE_INVALID);
    }

    MeetingRoom room = findRoom(request.meetingRoomId());
    authorizationService.requireSessionHost(room.getId(), request.createdByUserId());
    requireActiveRoom(room);

    String occurrenceId = normalizeScheduleOccurrenceId(request.scheduleOccurrenceId());
    String title = normalizeTitle(request.title());
    validateScheduleTime(request.scheduledStartAt(), request.scheduledEndAt());

    Optional<MeetingSession> existing = sessionRepository.findByScheduleOccurrenceId(occurrenceId);
    if (existing.isPresent()) {
      return synchronize(existing.get(), room.getId(), request, title);
    }

    MeetingSession session = new MeetingSession(
        room.getId(),
        title,
        generateProviderRoomName(),
        occurrenceId,
        request.scheduledStartAt(),
        request.scheduledEndAt(),
        request.createdByUserId()
    );
    return sessionMapper.toResponse(saveScheduleSync(session));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingSessionResponse> getByRoom(String roomId, Long currentUserId) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireActiveMember(room.getId(), currentUserId);
    return sessionRepository.findAllByMeetingRoomIdOrderByScheduledStartAtAsc(room.getId())
        .stream()
        .map(sessionMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public MeetingSessionResponse getById(String sessionId, Long currentUserId) {
    MeetingSession session = findSession(sessionId);
    authorizationService.requireActiveMember(session.getMeetingRoomId(), currentUserId);
    return sessionMapper.toResponse(session);
  }

  @Override
  @Transactional
  public MeetingSessionResponse start(String sessionId, Long currentUserId) {
    MeetingSession session = findManagedSession(sessionId, currentUserId);
    MeetingRoom room = findRoom(session.getMeetingRoomId());
    requireActiveRoom(room);
    requireStatus(session, MeetingSessionStatus.SCHEDULED);

    session.setStatus(MeetingSessionStatus.ACTIVE);
    session.setActualStartAt(Instant.now());
    return sessionMapper.toResponse(saveTransition(session));
  }

  @Override
  @Transactional
  public MeetingSessionResponse end(String sessionId, Long currentUserId) {
    MeetingSession session = findManagedSession(sessionId, currentUserId);
    requireStatus(session, MeetingSessionStatus.ACTIVE);

    session.setStatus(MeetingSessionStatus.ENDED);
    session.setActualEndAt(Instant.now());
    MeetingSession endedSession = saveTransition(session);
    eventPublisher.publishEvent(new MeetingSessionEndedEvent(
        endedSession.getId(),
        endedSession.getProviderRoomName()
    ));
    return sessionMapper.toResponse(endedSession);
  }

  @Override
  @Transactional
  public MeetingSessionResponse cancel(String sessionId, Long currentUserId) {
    MeetingSession session = findManagedSession(sessionId, currentUserId);
    requireStatus(session, MeetingSessionStatus.SCHEDULED);

    session.setStatus(MeetingSessionStatus.CANCELLED);
    return sessionMapper.toResponse(saveTransition(session));
  }

  private MeetingSessionResponse synchronize(
      MeetingSession session,
      String roomId,
      MeetingSessionScheduleSyncRequest request,
      String title
  ) {
    if (!session.getMeetingRoomId().equals(roomId)) {
      throw BusinessException.existed(
          MeetingErrorConstants.SESSION_SCHEDULE_CONFLICT,
          request.scheduleOccurrenceId()
      );
    }
    if (session.getStatus() != MeetingSessionStatus.SCHEDULED) {
      return sessionMapper.toResponse(session);
    }

    session.setTitle(title);
    session.setScheduledStartAt(request.scheduledStartAt());
    session.setScheduledEndAt(request.scheduledEndAt());
    return sessionMapper.toResponse(saveScheduleSync(session));
  }

  private MeetingSession saveScheduleSync(MeetingSession session) {
    try {
      return sessionRepository.saveAndFlush(session);
    } catch (DataIntegrityViolationException exception) {
      throw BusinessException.existed(
          MeetingErrorConstants.SESSION_SCHEDULE_CONFLICT,
          session.getScheduleOccurrenceId()
      );
    } catch (OptimisticLockingFailureException exception) {
      throw BusinessException.invalidInput(MeetingErrorConstants.SESSION_CONCURRENT_UPDATE);
    }
  }

  private MeetingSession saveTransition(MeetingSession session) {
    try {
      return sessionRepository.saveAndFlush(session);
    } catch (OptimisticLockingFailureException exception) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.SESSION_CONCURRENT_UPDATE,
          session.getId()
      );
    }
  }

  private MeetingSession findManagedSession(String sessionId, Long currentUserId) {
    MeetingSession session = findSession(sessionId);
    authorizationService.requireSessionHost(session.getMeetingRoomId(), currentUserId);
    return session;
  }

  private MeetingSession findSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.SESSION_NOT_FOUND);
    }
    return sessionRepository.findById(sessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.SESSION_NOT_FOUND,
            sessionId
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

  private void requireStatus(MeetingSession session, MeetingSessionStatus expectedStatus) {
    if (session.getStatus() != expectedStatus) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.SESSION_STATE_INVALID,
          session.getStatus(),
          expectedStatus
      );
    }
  }

  private String normalizeTitle(String title) {
    if (title == null || title.isBlank() || title.trim().length() > TITLE_MAX_LENGTH) {
      throw BusinessException.invalidInput(MeetingErrorConstants.SESSION_TITLE_INVALID);
    }
    return title.trim();
  }

  private String normalizeScheduleOccurrenceId(String occurrenceId) {
    if (occurrenceId == null
        || occurrenceId.isBlank()
        || occurrenceId.trim().length() > SCHEDULE_OCCURRENCE_MAX_LENGTH) {
      throw BusinessException.invalidInput(MeetingErrorConstants.SESSION_SCHEDULE_INVALID);
    }
    return occurrenceId.trim();
  }

  private void validateScheduleTime(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.SESSION_TIME_INVALID);
    }
  }

  private String generateProviderRoomName() {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    return "session-" + suffix.toLowerCase(Locale.ROOT);
  }
}
