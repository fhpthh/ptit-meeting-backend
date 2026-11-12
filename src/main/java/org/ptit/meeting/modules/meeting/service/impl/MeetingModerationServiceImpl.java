package org.ptit.meeting.modules.meeting.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionBanRequest;
import org.ptit.meeting.modules.meeting.dto.request.ParticipantTrackMuteRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanListItemResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanResponse;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.entity.MeetingSessionBan;
import org.ptit.meeting.modules.meeting.event.MeetingParticipantRemovedEvent;
import org.ptit.meeting.modules.meeting.mapper.MeetingSessionBanMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingSessionBanRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingModerationServiceImpl implements MeetingModerationService {

  private static final String PARTICIPANT_IDENTITY_PREFIX = "user-";

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingSessionBanRepository banRepository;
  private final MeetingAuthorizationService authorizationService;
  private final UserService userService;
  private final LiveKitGateway liveKitGateway;
  private final MeetingSessionBanMapper banMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional(readOnly = true)
  public void muteTrack(
      String runtimeSessionId,
      Long participantUserId,
      ParticipantTrackMuteRequest request,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = requireActiveSession(runtimeSessionId);
    requireModerationPermission(runtimeSession, currentUserId, participantUserId);

    boolean muted = callProvider(() -> liveKitGateway.mutePublishedTrack(
        runtimeSession.getProviderRoomName(),
        participantIdentity(participantUserId),
        request.trackSid()
    ));
    if (!muted) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.MODERATION_PARTICIPANT_NOT_CONNECTED
      );
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void kick(
      String runtimeSessionId,
      Long participantUserId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = requireActiveSession(runtimeSessionId);
    requireModerationPermission(runtimeSession, currentUserId, participantUserId);

    boolean removed = callProvider(() -> liveKitGateway.removeParticipant(
        runtimeSession.getProviderRoomName(),
        participantIdentity(participantUserId)
    ));
    if (!removed) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.MODERATION_PARTICIPANT_NOT_CONNECTED
      );
    }
  }

  @Override
  @Transactional
  public MeetingSessionBanResponse ban(
      String runtimeSessionId,
      Long participantUserId,
      MeetingSessionBanRequest request,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = requireActiveSession(runtimeSessionId);
    requireModerationPermission(runtimeSession, currentUserId, participantUserId);

    MeetingSessionBan ban = banRepository
        .findByRuntimeSessionIdAndUserId(runtimeSessionId, participantUserId)
        .orElseGet(() -> new MeetingSessionBan(
            runtimeSessionId,
            participantUserId,
            currentUserId,
            normalizeReason(request.reason())
        ));
    if (!ban.isActive()) {
      ban.applyBan(currentUserId, normalizeReason(request.reason()));
    }
    MeetingSessionBan savedBan = banRepository.save(ban);

    eventPublisher.publishEvent(new MeetingParticipantRemovedEvent(
        runtimeSession.getProviderRoomName(),
        participantIdentity(participantUserId)
    ));
    return banMapper.toResponse(savedBan);
  }

  @Override
  @Transactional
  public MeetingSessionBanResponse unban(
      String runtimeSessionId,
      Long participantUserId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = requireActiveSession(runtimeSessionId);
    requireModerationPermission(runtimeSession, currentUserId, participantUserId);

    MeetingSessionBan ban = banRepository
        .findByRuntimeSessionIdAndUserId(runtimeSessionId, participantUserId)
        .filter(MeetingSessionBan::isActive)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.SESSION_BAN_NOT_FOUND
        ));
    ban.revoke(currentUserId);
    return banMapper.toResponse(banRepository.save(ban));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingSessionBanListItemResponse> getActiveBans(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = findSession(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );

    List<MeetingSessionBan> bans = banRepository
        .findAllByRuntimeSessionIdAndRevokedAtIsNullOrderByBannedAtDesc(
            runtimeSessionId
        );
    Map<Long, User> usersById = userService.getUsersByIds(
        bans.stream().map(MeetingSessionBan::getUserId).toList()
    ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

    return bans.stream()
        .map(ban -> banMapper.toListItem(ban, requireUser(usersById, ban.getUserId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public void requireNotBanned(String runtimeSessionId, Long userId) {
    if (banRepository.existsByRuntimeSessionIdAndUserIdAndRevokedAtIsNull(
        runtimeSessionId,
        userId
    )) {
      throw BusinessException.unauthorized(
          MeetingErrorConstants.SESSION_PARTICIPANT_BANNED
      );
    }
  }

  @Override
  @Transactional(readOnly = true)
  public boolean enforceBan(String runtimeSessionId, Long userId) {
    if (!banRepository.existsByRuntimeSessionIdAndUserIdAndRevokedAtIsNull(
        runtimeSessionId,
        userId
    )) {
      return false;
    }

    MeetingSession runtimeSession = findSession(runtimeSessionId);
    callProvider(() -> liveKitGateway.removeParticipant(
        runtimeSession.getProviderRoomName(),
        participantIdentity(userId)
    ));
    return true;
  }

  private MeetingSession requireActiveSession(String runtimeSessionId) {
    MeetingSession runtimeSession = findSession(runtimeSessionId);
    if (runtimeSession.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSessionId
      );
    }
    return runtimeSession;
  }

  private MeetingSession findSession(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND);
    }
    return runtimeSessionRepository.findByPublicId(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND,
            runtimeSessionId
        ));
  }

  private void requireModerationPermission(
      MeetingSession runtimeSession,
      Long currentUserId,
      Long participantUserId
  ) {
    if (currentUserId != null && currentUserId.equals(participantUserId)) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MODERATION_SELF_ACTION);
    }

    ClassEnrollment actor = authorizationService.requireActiveMember(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    ClassEnrollment target = authorizationService.requireActiveMember(
        runtimeSession.getMeetingRoomId(),
        participantUserId
    );
    if (actor.getRole() != MeetingRole.OWNER && actor.getRole() != MeetingRole.HOST) {
      throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
    }
    if (target.getRole() == MeetingRole.OWNER
        || (actor.getRole() == MeetingRole.HOST && target.getRole() == MeetingRole.HOST)) {
      throw BusinessException.unauthorized(
          MeetingErrorConstants.MODERATION_TARGET_PROTECTED
      );
    }
  }

  private boolean callProvider(ProviderOperation operation) {
    try {
      return operation.execute();
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.PROVIDER_UNAVAILABLE
      );
    }
  }

  private String participantIdentity(Long userId) {
    return PARTICIPANT_IDENTITY_PREFIX + userId;
  }

  private String normalizeReason(String reason) {
    return reason == null || reason.isBlank() ? null : reason.trim();
  }

  private User requireUser(Map<Long, User> usersById, Long userId) {
    User user = usersById.get(userId);
    if (user == null) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.MEMBER_NOT_FOUND,
          userId
      );
    }
    return user;
  }

  @FunctionalInterface
  private interface ProviderOperation {

    boolean execute();
  }
}
