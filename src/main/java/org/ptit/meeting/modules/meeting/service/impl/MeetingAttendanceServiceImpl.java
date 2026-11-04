package org.ptit.meeting.modules.meeting.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.ParticipantConnectionStatus;
import org.ptit.meeting.modules.meeting.dto.internal.ParticipantConnectionEvent;
import org.ptit.meeting.modules.meeting.dto.response.MeetingAttendanceResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.ptit.meeting.modules.meeting.entity.ParticipantConnection;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.repository.ParticipantConnectionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAttendanceService;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingAttendanceServiceImpl implements MeetingAttendanceService {

  private static final int DISCONNECT_REASON_MAX_LENGTH = 100;

  private final ParticipantConnectionRepository connectionRepository;
  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingAuthorizationService authorizationService;

  @Override
  @Transactional
  public void recordJoined(ParticipantConnectionEvent event) {
    ParticipantConnection connection = findConnection(event)
        .orElseGet(() -> newConnection(event));
    connection.markJoined(event.occurredAt());
    connectionRepository.saveAndFlush(connection);
  }

  @Override
  @Transactional
  public void recordLeft(ParticipantConnectionEvent event) {
    ParticipantConnection connection = findConnection(event)
        .orElseGet(() -> newConnection(event));
    connection.markLeft(event.occurredAt(), normalizeReason(event.disconnectReason()));
    connectionRepository.saveAndFlush(connection);
  }

  @Override
  @Transactional
  public void recordAborted(ParticipantConnectionEvent event) {
    ParticipantConnection connection = findConnection(event)
        .orElseGet(() -> newConnection(event));
    connection.markAborted(event.occurredAt(), normalizeReason(event.disconnectReason()));
    connectionRepository.saveAndFlush(connection);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingAttendanceResponse> getAttendance(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingRuntimeSession runtimeSession = findRuntimeSession(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );

    Map<Long, List<ParticipantConnection>> connectionsByUser = connectionRepository
        .findAllByRuntimeSessionId(runtimeSessionId)
        .stream()
        .collect(Collectors.groupingBy(
            ParticipantConnection::getUserId,
            TreeMap::new,
            Collectors.toList()
        ));

    Instant calculatedAt = runtimeSession.getActualEndAt() == null
        ? Instant.now()
        : runtimeSession.getActualEndAt();
    boolean sessionIsActive = runtimeSession.getStatus()
        == MeetingRuntimeSessionStatus.ACTIVE;
    return connectionsByUser.entrySet().stream()
        .map(entry -> summarize(
            entry.getKey(),
            entry.getValue(),
            calculatedAt,
            sessionIsActive
        ))
        .filter(response -> response.connectionCount() > 0)
        .toList();
  }

  private Optional<ParticipantConnection> findConnection(ParticipantConnectionEvent event) {
    return connectionRepository.findByRuntimeSessionIdAndParticipantSid(
        event.runtimeSessionId(),
        event.participantSid()
    );
  }

  private ParticipantConnection newConnection(ParticipantConnectionEvent event) {
    return new ParticipantConnection(
        event.runtimeSessionId(),
        event.userId(),
        event.participantIdentity(),
        event.participantSid()
    );
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

  private MeetingAttendanceResponse summarize(
      Long userId,
      List<ParticipantConnection> connections,
      Instant calculatedAt,
      boolean sessionIsActive
  ) {
    List<TimeInterval> intervals = connections.stream()
        .filter(connection -> connection.getJoinedAt() != null)
        .map(connection -> toInterval(connection, calculatedAt))
        .sorted(Comparator.comparing(TimeInterval::start))
        .toList();

    Instant firstJoinedAt = intervals.isEmpty() ? null : intervals.getFirst().start();
    Instant lastLeftAt = connections.stream()
        .map(ParticipantConnection::getLeftAt)
        .filter(java.util.Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
    boolean currentlyOnline = sessionIsActive && connections.stream()
        .anyMatch(connection ->
            connection.getStatus() == ParticipantConnectionStatus.ACTIVE
                && connection.getLeftAt() == null
        );

    return new MeetingAttendanceResponse(
        userId,
        firstJoinedAt,
        lastLeftAt,
        unionDurationSeconds(intervals),
        intervals.size(),
        currentlyOnline
    );
  }

  private TimeInterval toInterval(
      ParticipantConnection connection,
      Instant calculatedAt
  ) {
    Instant end = connection.getLeftAt() == null ? calculatedAt : connection.getLeftAt();
    if (end.isBefore(connection.getJoinedAt())) {
      end = connection.getJoinedAt();
    }
    return new TimeInterval(connection.getJoinedAt(), end);
  }

  private long unionDurationSeconds(List<TimeInterval> intervals) {
    if (intervals.isEmpty()) {
      return 0L;
    }

    List<TimeInterval> merged = new ArrayList<>();
    TimeInterval current = intervals.getFirst();
    for (int index = 1; index < intervals.size(); index++) {
      TimeInterval next = intervals.get(index);
      if (!next.start().isAfter(current.end())) {
        current = new TimeInterval(current.start(), laterOf(current.end(), next.end()));
      } else {
        merged.add(current);
        current = next;
      }
    }
    merged.add(current);

    return merged.stream()
        .mapToLong(interval -> Duration.between(interval.start(), interval.end()).getSeconds())
        .sum();
  }

  private Instant laterOf(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }

  private String normalizeReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    String normalized = reason.trim();
    return normalized.length() <= DISCONNECT_REASON_MAX_LENGTH
        ? normalized
        : normalized.substring(0, DISCONNECT_REASON_MAX_LENGTH);
  }

  private record TimeInterval(Instant start, Instant end) {
  }
}
