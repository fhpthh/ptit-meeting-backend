package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.ai.service.AiWorkerAuthorizationService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.dto.response.MeetingJoinResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.event.MeetingRuntimeSessionEndedEvent;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingJoinServiceImpl implements MeetingJoinService {

  private static final String EVENT_SPEAKING_SID = "eventSpeakingSid";

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingRoomService roomService;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingModerationService moderationService;
  private final UserService userService;
  private final ObjectMapper objectMapper;
  private final LiveKitGateway liveKitGateway;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final MeetingRuntimeSessionService runtimeSessionService;
  private final AiWorkerAuthorizationService aiWorkerAuthorizationService;

  @Override
  @Transactional
  public MeetingJoinResponse join(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = findRuntimeSession(runtimeSessionId);
    MeetingRole role = authorizationService.requireRoomAccess(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    requireActiveRuntimeSession(runtimeSession);
    moderationService.requireNotBanned(runtimeSession.getPublicId(), currentUserId);

    Room room = findRoom(runtimeSession.getMeetingRoomId());
    requireActiveRoom(room);
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    requireAllowedJoinTime(runtimeSession, setting, role);

    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);

    String participantIdentity = "user-" + currentUserId;
    LiveKitJoinGrant grant = createGrant(runtimeSession, room, setting, role, user, false, null);
    requireCapacity(runtimeSession, setting, participantIdentity);
    LiveKitTokenResult tokenResult = createJoinToken(grant);

    return new MeetingJoinResponse(
        runtimeSession.getPublicId(),
        room.getPublicId(),
        tokenResult.websocketUrl(),
        tokenResult.participantToken(),
        tokenResult.expiresAt(),
        participantIdentity,
        role,
        true,
        grant.canPublishMicrophone(),
        grant.canPublishCamera(),
        screenShareAllowed(role, setting)
    );
  }

  private LiveKitJoinGrant createGrant(MeetingSession session, Room room, MeetingRoomSetting setting,
      MeetingRole role, User user, boolean screenShareAcquired, String eventSpeakingSid) {
    boolean host = role == MeetingRole.OWNER || role == MeetingRole.HOST;
    boolean participant = role != MeetingRole.VIEWER;
    String speakingSid = room.getRoomType() == RoomType.EVENT && role == MeetingRole.VIEWER
        && (setting.isParticipantMicEnabled() || setting.isParticipantCameraEnabled())
        ? eventSpeakingSid : null;
    boolean mediaAllowed = participant || speakingSid != null;
    return new LiveKitJoinGrant(session.getProviderRoomName(), "user-" + user.getId(),
        user.getFullName(), createParticipantMetadata(session, room, role, user.getId(), setting,
            speakingSid), true,
        host || mediaAllowed && setting.isParticipantMicEnabled(),
        host || mediaAllowed && setting.isParticipantCameraEnabled(),
        screenShareAcquired && screenShareAllowed(role, setting), true);
  }

  @Override
  @Transactional
  public void acquireScreenShare(
      String runtimeSessionId,
      String participantSid,
      Long currentUserId
  ) {
    MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
    requireActiveRuntimeSession(session);
    Room room = findRoom(session.getMeetingRoomId());
    requireActiveRoom(room);
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    MeetingRole role = authorizationService.requireRoomAccess(
        room.getPublicId(), currentUserId);
    moderationService.requireNotBanned(runtimeSessionId, currentUserId);
    if (!screenShareAllowed(role, setting)) {
      throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
    }
    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);
    try {
      List<LiveKitParticipantSnapshot> participants = orderedParticipants(session);
      LiveKitParticipantSnapshot participant = requireConnectedParticipant(
          participants, currentUserId, participantSid);
      boolean occupied = participants.stream().anyMatch(value ->
          !value.sid().equals(participant.sid()) && value.publishSources().contains("screen_share"));
      if (occupied) {
        throw BusinessException.existed(MeetingErrorConstants.SCREEN_SHARE_BUSY);
      }
      LiveKitJoinGrant grant = createGrant(session, room, setting, role, user, true, null);
      updateConnectedParticipant(grant);
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND, MeetingErrorConstants.PROVIDER_UNAVAILABLE);
    }
  }

  @Override
  @Transactional
  public void releaseScreenShare(
      String runtimeSessionId,
      String participantSid,
      Long currentUserId
  ) {
    MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
    try {
      for (LiveKitParticipantSnapshot participant : orderedParticipants(session)) {
        if (participant.identity().equals("user-" + currentUserId)
            && participant.sid().equals(participantSid)
            && participant.publishSources().contains("screen_share")) {
          // Revoke this connection's screen sources without restoring stale mic/camera rights.
          liveKitGateway.updateParticipant(new LiveKitJoinGrant(
              session.getProviderRoomName(), participant.identity(),
              userService.getUserById(currentUserId).getFullName(), participant.metadata(),
              participant.canSubscribe(), participant.publishSources().contains("microphone"),
              participant.publishSources().contains("camera"), false, true));
        }
      }
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND, MeetingErrorConstants.PROVIDER_UNAVAILABLE);
    }
  }

  @Override
  @Transactional
  public void setEventSpeakingPermission(String runtimeSessionId, Long participantUserId,
      String participantSid, boolean enabled, Long currentUserId) {
    MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
    Room room = findRoom(session.getMeetingRoomId());
    authorizationService.requireRuntimeSessionHost(room.getPublicId(), currentUserId);
    moderationService.requireNotBanned(runtimeSessionId, currentUserId);
    userService.validateUserStatus(userService.getUserById(currentUserId));
    requireActiveRuntimeSession(session);
    requireActiveRoom(room);
    if (room.getRoomType() != RoomType.EVENT) {
      throw BusinessException.invalidInput(MeetingErrorConstants.EVENT_SPEAKING_NOT_SUPPORTED);
    }
    MeetingRole role = authorizationService.requireRoomAccess(room.getPublicId(), participantUserId);
    if (role != MeetingRole.VIEWER) {
      throw BusinessException.unauthorized(MeetingErrorConstants.MODERATION_TARGET_PROTECTED);
    }
    moderationService.requireNotBanned(runtimeSessionId, participantUserId);
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    if (enabled && !setting.isParticipantMicEnabled() && !setting.isParticipantCameraEnabled()) {
      throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
    }
    User user = userService.getUserById(participantUserId);
    userService.validateUserStatus(user);
    try {
      LiveKitParticipantSnapshot participant = requireConnectedParticipant(
          orderedParticipants(session), participantUserId, participantSid);
      updateConnectedParticipant(createGrant(session, room, setting, role, user, false,
          enabled ? participant.sid() : null));
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND, MeetingErrorConstants.PROVIDER_UNAVAILABLE);
    }
  }

  private LiveKitParticipantSnapshot requireConnectedParticipant(
      List<LiveKitParticipantSnapshot> participants, Long userId, String participantSid) {
    return participants.stream()
        .filter(participant -> participant.identity().equals("user-" + userId))
        .filter(participant -> participant.sid().equals(participantSid))
        .findFirst()
        .orElseThrow(() -> BusinessException.invalidInput(
            MeetingErrorConstants.MODERATION_PARTICIPANT_NOT_CONNECTED));
  }

  private void updateConnectedParticipant(LiveKitJoinGrant grant) {
    if (!liveKitGateway.updateParticipant(grant)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MODERATION_PARTICIPANT_NOT_CONNECTED);
    }
  }

  private boolean screenShareAllowed(MeetingRole role, MeetingRoomSetting setting) {
    return role == MeetingRole.OWNER || role == MeetingRole.HOST
        || role != MeetingRole.VIEWER && setting.isParticipantScreenShareEnabled();
  }

  private void requireCapacity(MeetingSession session, MeetingRoomSetting setting, String identity) {
    try {
      liveKitGateway.ensureRoom(session.getProviderRoomName(), MeetingRoomSetting.MAX_PARTICIPANTS);
      List<LiveKitParticipantSnapshot> participants = liveKitGateway
          .listParticipants(session.getProviderRoomName()).stream()
          .filter(value -> !aiWorkerAuthorizationService.isAuthorized(session.getPublicId(), value))
          .toList();
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
    MeetingSession session = findRuntimeSessionForUpdate(runtimeSessionId);
    Room room = findRoom(session.getMeetingRoomId());
    String identity = "user-" + userId;
    if (session.getRuntimeStatus() == MeetingRuntimeSessionStatus.ENDED) {
      // Persist the retry before attempting to close a room recreated by an old token.
      session.setCleanupPending(true);
      eventPublisher.publishEvent(new MeetingRuntimeSessionEndedEvent(runtimeSessionId));
      return false;
    }
    if (session.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE
        || room.getStatus() != RoomStatus.ACTIVE) {
      liveKitGateway.removeParticipant(session.getProviderRoomName(), identity);
      return false;
    }
    MeetingRoomSetting setting = roomService.getSetting(room.getPublicId());
    List<LiveKitParticipantSnapshot> participants = orderedParticipants(session);
    LiveKitParticipantSnapshot participant = participants.stream()
        .filter(value -> value.identity().equals(identity)).findFirst().orElse(null);
    if (participant == null) {
      return false;
    }
    boolean screenShareAcquired = participant.publishSources().contains("screen_share")
        && participants.stream().noneMatch(value -> !value.sid().equals(participant.sid())
            && value.publishSources().contains("screen_share"));
    LiveKitJoinGrant grant = currentGrant(session, room, setting, participant, screenShareAcquired);
    boolean withinCapacity = participants.stream()
        .filter(value -> !aiWorkerAuthorizationService.isAuthorized(session.getPublicId(), value))
        .limit(setting.getMaxParticipants())
        .anyMatch(value -> value.identity().equals(identity));
    if (grant == null || !withinCapacity) {
      liveKitGateway.removeParticipant(session.getProviderRoomName(), identity);
      return false;
    }
    updateAccessIfChanged(participant, grant);
    return true;
  }

  @Override
  public void synchronizeRoom(String roomId) {
    Room room = findRoom(roomId);
    MeetingRoomSetting setting = roomService.getSetting(roomId);
    for (MeetingSession session : runtimeSessionRepository.findAllByRoomIdAndStatus(room.getId(), SessionStatus.LIVE)) {
      List<LiveKitParticipantSnapshot> participants = room.getRoomType() == RoomType.EVENT
          ? List.of() : orderedParticipants(session);
      transactionTemplate.executeWithoutResult(status -> synchronizeSession(session, room, setting, participants));
    }
  }

  @Override
  public void reconcileActiveSessions() {
    for (MeetingSession session : runtimeSessionRepository.findAllByStatus(SessionStatus.LIVE)) {
      try {
        if (session.getRoom().getRoomType() == RoomType.EVENT) {
          List<LiveKitParticipantSnapshot> participants = transactionTemplate.execute(status -> {
            Room room = session.getRoom();
            return synchronizeSession(session, room, roomService.getSetting(room.getPublicId()), List.of());
          });
          reconcileOvertime(session, participants);
          continue;
        }
        // Share one LiveKit snapshot between overtime checks and permission reconciliation.
        List<LiveKitParticipantSnapshot> participants = orderedParticipants(session);
        reconcileOvertime(session, participants);
        transactionTemplate.executeWithoutResult(status -> {
          Room room = session.getRoom();
          synchronizeSession(session, room, roomService.getSetting(room.getPublicId()), participants);
        });
      } catch (RuntimeException exception) {
        log.warn("Unable to reconcile LiveKit session; will retry: runtimeSessionId={}",
            session.getPublicId(), exception);
      }
    }
  }

  private void reconcileOvertime(MeetingSession session,
      List<LiveKitParticipantSnapshot> participants) {
    if (session.getScheduledEndAt().isAfter(Instant.now())) {
      return;
    }
    try {
      boolean hasParticipants = participants.stream()
          .anyMatch(value -> !aiWorkerAuthorizationService.isAuthorized(session.getPublicId(), value));
      runtimeSessionService.reconcileOvertime(
          session.getPublicId(), session.getVersion(), hasParticipants
      );
    } catch (RuntimeException exception) {
      log.warn("Unable to reconcile overtime; access reconciliation will continue: runtimeSessionId={}",
          session.getPublicId(), exception);
    }
  }

  private List<LiveKitParticipantSnapshot> synchronizeSession(
      MeetingSession session, Room room, MeetingRoomSetting setting,
      List<LiveKitParticipantSnapshot> participants) {
    session = findRuntimeSessionForUpdate(session.getPublicId());
    if (session.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      return List.of();
    }
    if (room.getRoomType() == RoomType.EVENT) {
      // Read temporary grants under the same lock as grant/revoke to avoid restoring stale access.
      participants = orderedParticipants(session);
    }
    int admitted = 0;
    boolean screenShareAssigned = false;
    for (LiveKitParticipantSnapshot participant : participants) {
      if (aiWorkerAuthorizationService.isAuthorized(session.getPublicId(), participant)) {
        continue;
      }
      LiveKitJoinGrant grant = room.getStatus() != RoomStatus.ACTIVE
          ? null : currentGrant(session, room, setting, participant,
              !screenShareAssigned && participant.publishSources().contains("screen_share"));
      if (grant == null || admitted >= setting.getMaxParticipants()) {
        liveKitGateway.removeParticipant(session.getProviderRoomName(), participant.identity());
      } else {
        admitted++;
        screenShareAssigned = screenShareAssigned || grant.canShareScreen();
        updateAccessIfChanged(participant, grant);
      }
    }
    return participants;
  }

  private LiveKitJoinGrant currentGrant(MeetingSession session, Room room, MeetingRoomSetting setting,
      LiveKitParticipantSnapshot participant, boolean screenShareAcquired) {
    Long userId = userIdFromIdentity(participant.identity());
    if (userId == null) return null;
    try {
      MeetingRole role = authorizationService.requireRoomAccess(room.getPublicId(), userId);
      moderationService.requireNotBanned(session.getPublicId(), userId);
      User user = userService.getUserById(userId);
      userService.validateUserStatus(user);
      return createGrant(session, room, setting, role, user, screenShareAcquired,
          eventSpeakingSid(session, participant));
    } catch (BusinessException exception) {
      return null;
    }
  }

  private String eventSpeakingSid(MeetingSession session, LiveKitParticipantSnapshot participant) {
    if (session.getRoom().getRoomType() != RoomType.EVENT || participant.metadata() == null) {
      return null;
    }
    try {
      var metadata = objectMapper.readTree(participant.metadata());
      return metadata != null
          && session.getPublicId().equals(metadata.path("runtimeSessionId").asText())
          && participant.sid().equals(metadata.path(EVENT_SPEAKING_SID).asText())
          ? participant.sid() : null;
    } catch (JsonProcessingException exception) {
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

  private MeetingSession findRuntimeSessionForUpdate(String runtimeSessionId) {
    return runtimeSessionRepository.findByPublicIdForUpdate(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND, runtimeSessionId));
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
      MeetingRole role,
      Long userId,
      MeetingRoomSetting setting,
      String eventSpeakingSid
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("userId", userId);
    metadata.put("runtimeSessionId", runtimeSession.getPublicId());
    metadata.put("roomId", room.getPublicId());
    metadata.put("meetingRole", role.name());
    metadata.put("canShareScreen", screenShareAllowed(role, setting));
    if (eventSpeakingSid != null) {
      metadata.put(EVENT_SPEAKING_SID, eventSpeakingSid);
    }

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
