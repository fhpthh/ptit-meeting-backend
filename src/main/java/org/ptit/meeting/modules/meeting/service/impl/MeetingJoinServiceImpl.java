package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
import org.ptit.meeting.modules.meeting.constant.enums.MeetingSessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.dto.response.MeetingJoinResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomSettingRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingJoinServiceImpl implements MeetingJoinService {

  private final MeetingSessionRepository sessionRepository;
  private final MeetingRoomRepository roomRepository;
  private final MeetingRoomSettingRepository settingRepository;
  private final MeetingAuthorizationService authorizationService;
  private final UserService userService;
  private final ObjectMapper objectMapper;
  private final LiveKitGateway liveKitGateway;

  @Override
  @Transactional(readOnly = true)
  public MeetingJoinResponse join(String sessionId, Long currentUserId) {
    MeetingSession session = findSession(sessionId);
    MeetingRoomMember member = authorizationService.requireActiveMember(
        session.getMeetingRoomId(),
        currentUserId
    );
    requireActiveSession(session);

    MeetingRoom room = findRoom(session.getMeetingRoomId());
    requireActiveRoom(room);
    MeetingRoomSetting setting = findSetting(room.getId());
    requireAllowedJoinTime(session, setting, member.getRole());

    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);

    boolean viewer = member.getRole() == MeetingRole.VIEWER;
    boolean canPublishMicrophone = !viewer && setting.isParticipantMicEnabled();
    boolean canPublishCamera = !viewer && setting.isParticipantCameraEnabled();
    boolean canShareScreen = !viewer && setting.isParticipantScreenShareEnabled();
    String participantIdentity = "user-" + currentUserId;

    LiveKitJoinGrant grant = new LiveKitJoinGrant(
        session.getProviderRoomName(),
        participantIdentity,
        user.getFullName(),
        createParticipantMetadata(session, room, member),
        true,
        canPublishMicrophone,
        canPublishCamera,
        canShareScreen
    );
    LiveKitTokenResult tokenResult = createJoinToken(grant);

    return new MeetingJoinResponse(
        session.getId(),
        room.getId(),
        tokenResult.websocketUrl(),
        tokenResult.participantToken(),
        tokenResult.expiresAt(),
        participantIdentity,
        member.getRole(),
        true,
        canPublishMicrophone,
        canPublishCamera,
        canShareScreen
    );
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
    return roomRepository.findById(roomId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.ROOM_NOT_FOUND,
            roomId
        ));
  }

  private MeetingRoomSetting findSetting(String roomId) {
    return settingRepository.findByRoomId(roomId)
        .orElseThrow(() -> new BusinessException(
            StatusCode.ERROR_IN_BACKEND,
            MeetingErrorConstants.SETTING_NOT_FOUND,
            roomId
        ));
  }

  private void requireActiveSession(MeetingSession session) {
    if (session.getStatus() != MeetingSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.SESSION_NOT_ACTIVE,
          session.getId()
      );
    }
  }

  private void requireActiveRoom(MeetingRoom room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getId());
    }
  }

  private void requireAllowedJoinTime(
      MeetingSession session,
      MeetingRoomSetting setting,
      MeetingRole role
  ) {
    if (role == MeetingRole.OWNER || role == MeetingRole.HOST) {
      return;
    }

    Instant earliestJoin = session.getScheduledStartAt()
        .minusSeconds(setting.getJoinBeforeMinutes() * 60L);
    if (Instant.now().isBefore(earliestJoin)) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.JOIN_TOO_EARLY,
          earliestJoin
      );
    }
  }

  private String createParticipantMetadata(
      MeetingSession session,
      MeetingRoom room,
      MeetingRoomMember member
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("userId", member.getUserId());
    metadata.put("sessionId", session.getId());
    metadata.put("roomId", room.getId());
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
