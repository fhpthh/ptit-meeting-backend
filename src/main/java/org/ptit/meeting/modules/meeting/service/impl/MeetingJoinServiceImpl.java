package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.dto.response.MeetingJoinResponse;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingJoinServiceImpl implements MeetingJoinService {

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingRoomService roomService;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingModerationService moderationService;
  private final UserService userService;
  private final ObjectMapper objectMapper;
  private final LiveKitGateway liveKitGateway;

  @Override
  @Transactional
  public MeetingJoinResponse join(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findRuntimeSession(runtimeSessionId);
    ClassEnrollment member = authorizationService.requireActiveMember(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    requireActiveRuntimeSession(runtimeSession);
    moderationService.requireNotBanned(runtimeSession.getPublicId(), currentUserId);

    Room room = findRoom(runtimeSession.getMeetingRoomId());
    requireActiveRoom(room);
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    requireAllowedJoinTime(runtimeSession, setting, member.getRole());

    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);

    String participantIdentity = "user-" + currentUserId;
    LiveKitJoinGrant grant = createGrant(runtimeSession, room, setting, member, user);
    requireCapacity(runtimeSession, setting, participantIdentity);
    LiveKitTokenResult tokenResult = createJoinToken(grant);

    return new MeetingJoinResponse(
        runtimeSession.getPublicId(),
        room.getPublicId(),
        tokenResult.websocketUrl(),
        tokenResult.participantToken(),
        tokenResult.expiresAt(),
        participantIdentity,
        member.getRole(),
        true,
        grant.canPublishMicrophone(),
        grant.canPublishCamera(),
        grant.canShareScreen()
    );
  }

  private LiveKitJoinGrant createGrant(MeetingSession session, Room room, MeetingRoomSetting setting,
      ClassEnrollment member, User user) {
    boolean host = member.getRole() == MeetingRole.OWNER || member.getRole() == MeetingRole.HOST;
    boolean participant = member.getRole() != MeetingRole.VIEWER;
    return new LiveKitJoinGrant(session.getProviderRoomName(), "user-" + user.getId(),
        user.getFullName(), createParticipantMetadata(session, room, member), true,
        host || participant && setting.isParticipantMicEnabled(),
        host || participant && setting.isParticipantCameraEnabled(),
        host || participant && setting.isParticipantScreenShareEnabled());
  }

  private void requireCapacity(MeetingSession session, MeetingRoomSetting setting, String identity) {
    try {
      liveKitGateway.ensureRoom(session.getProviderRoomName(), MeetingRoomSetting.MAX_PARTICIPANTS);
      List<LiveKitParticipantSnapshot> participants = liveKitGateway.listParticipants(session.getProviderRoomName());
      if (participants.stream().noneMatch(participant -> participant.identity().equals(identity))
          && participants.size() >= setting.getMaxParticipants()) {
        throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_FULL, setting.getMaxParticipants());
      }
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(StatusCode.ERROR_IN_BACKEND, MeetingErrorConstants.PROVIDER_UNAVAILABLE);
    }
  }

  @Override
  @Transactional
  public boolean admitParticipant(String runtimeSessionId, Long userId) {
    MeetingSession session = findRuntimeSession(runtimeSessionId);
    Room room = findRoom(session.getMeetingRoomId());
    String identity = "user-" + userId;
    if (session.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE || room.getStatus() != RoomStatus.ACTIVE) {
      liveKitGateway.removeParticipant(session.getProviderRoomName(), identity);
      return false;
    }
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    List<LiveKitParticipantSnapshot> participants = orderedParticipants(session);
    LiveKitParticipantSnapshot participant = participants.stream()
        .filter(value -> value.identity().equals(identity)).findFirst().orElse(null);
    if (participant == null) return false;
    LiveKitJoinGrant grant = currentGrant(session, room, setting, userId);
    boolean withinCapacity = participants.stream().limit(setting.getMaxParticipants())
        .anyMatch(value -> value.identity().equals(identity));
    if (grant == null || !withinCapacity) {
      liveKitGateway.removeParticipant(session.getProviderRoomName(), identity);
      return false;
    }
    updateAccessIfChanged(participant, grant);
    return true;
  }

  @Override
  @Transactional
  public void synchronizeRoom(String roomId) {
    Room room = findRoom(roomId);
    MeetingRoomSetting setting = roomService.getSetting(roomId);
    for (MeetingSession session : runtimeSessionRepository.findAllByRoomIdAndStatus(room.getId(), SessionStatus.LIVE)) {
      synchronizeSession(session, room, setting);
    }
  }

  @Override
  @Transactional
  public void reconcileActiveSessions() {
    for (MeetingSession session : runtimeSessionRepository.findAllByStatus(SessionStatus.LIVE)) {
      try {
        Room room = session.getRoom();
        synchronizeSession(session, room, roomService.getSetting(room.getPublicId()));
      } catch (LiveKitIntegrationException exception) {
        log.warn("Unable to reconcile LiveKit access: runtimeSessionId={}", session.getPublicId());
      }
    }
  }

  private void synchronizeSession(MeetingSession session, Room room, MeetingRoomSetting setting) {
    int admitted = 0;
    for (LiveKitParticipantSnapshot participant : orderedParticipants(session)) {
      Long userId = userIdFromIdentity(participant.identity());
      LiveKitJoinGrant grant = userId == null || room.getStatus() != RoomStatus.ACTIVE
          ? null : currentGrant(session, room, setting, userId);
      if (grant == null || admitted >= setting.getMaxParticipants()) {
        liveKitGateway.removeParticipant(session.getProviderRoomName(), participant.identity());
      } else {
        admitted++;
        updateAccessIfChanged(participant, grant);
      }
    }
  }

  private LiveKitJoinGrant currentGrant(MeetingSession session, Room room, MeetingRoomSetting setting, Long userId) {
    try {
      ClassEnrollment member = authorizationService.requireActiveMember(room.getPublicId(), userId);
      moderationService.requireNotBanned(session.getPublicId(), userId);
      User user = userService.getUserById(userId);
      userService.validateUserStatus(user);
      return createGrant(session, room, setting, member, user);
    } catch (BusinessException exception) {
      return null;
    }
  }

  private void updateAccessIfChanged(LiveKitParticipantSnapshot participant, LiveKitJoinGrant grant) {
    if (!Objects.equals(participant.metadata(), grant.participantMetadata())
        || participant.canSubscribe() != grant.canSubscribe()
        || !participant.publishSources().equals(Set.copyOf(grant.publishSources()))) {
      liveKitGateway.updateParticipant(grant);
    }
  }

  private List<LiveKitParticipantSnapshot> orderedParticipants(MeetingSession session) {
    return liveKitGateway.listParticipants(session.getProviderRoomName()).stream()
        .sorted(Comparator.comparingLong(LiveKitParticipantSnapshot::joinedAt)
            .thenComparing(LiveKitParticipantSnapshot::identity)).toList();
  }

  private Long userIdFromIdentity(String identity) {
    if (identity == null || !identity.startsWith("user-")) return null;
    try {
      long userId = Long.parseLong(identity.substring(5));
      return userId > 0 ? userId : null;
    } catch (NumberFormatException exception) {
      return null;
    }
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
    return roomRepository.findByPublicId(roomId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.ROOM_NOT_FOUND,
            roomId
        ));
  }

  private void requireActiveRuntimeSession(MeetingSession runtimeSession) {
    if (runtimeSession.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSession.getPublicId()
      );
    }
  }

  private void requireActiveRoom(Room room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getPublicId());
    }
  }

  private void requireAllowedJoinTime(
      MeetingSession runtimeSession,
      MeetingRoomSetting setting,
      MeetingRole role
  ) {
    if (role == MeetingRole.OWNER || role == MeetingRole.HOST) {
      return;
    }

    Instant earliestJoin = runtimeSession.getScheduledStartAt()
        .minusSeconds(setting.getJoinBeforeMinutes() * 60L);
    if (Instant.now().isBefore(earliestJoin)) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.JOIN_TOO_EARLY,
          earliestJoin
      );
    }
  }

  private String createParticipantMetadata(
      MeetingSession runtimeSession,
      Room room,
      ClassEnrollment member
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("userId", member.getUserId());
    metadata.put("runtimeSessionId", runtimeSession.getPublicId());
    metadata.put("roomId", room.getPublicId());
    metadata.put("meetingRole", member.getRole().name());

    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.JOIN_FAILED
      );
    }
  }

  private LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant) {
    try {
      return liveKitGateway.createJoinToken(grant);
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.PROVIDER_UNAVAILABLE
      );
    }
  }
}
