package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.response.MeetingHandRaiseResponse;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingHandRaiseService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingHandRaiseServiceImpl implements MeetingHandRaiseService {

  private static final String HAND_RAISE_TOPIC = "meeting.hand-raise.v1";
  private static final long HAND_RAISE_TTL_SECONDS = 24 * 60 * 60L;

  private static final DefaultRedisScript<String> RAISE_SCRIPT =
      new DefaultRedisScript<>("""
          local existing = redis.call('HGET', KEYS[2], ARGV[1])
          if existing then
            return cjson.encode({payload = existing})
          end

          redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
          redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
          redis.call('EXPIRE', KEYS[1], ARGV[4])
          redis.call('EXPIRE', KEYS[2], ARGV[4])
          return cjson.encode({payload = ARGV[3]})
          """, String.class);

  private static final DefaultRedisScript<String> LOWER_SCRIPT =
      new DefaultRedisScript<>("""
          local existing = redis.call('HGET', KEYS[2], ARGV[1])
          redis.call('ZREM', KEYS[1], ARGV[1])
          redis.call('HDEL', KEYS[2], ARGV[1])
          return existing or ''
          """, String.class);

  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingModerationService moderationService;
  private final UserService userService;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final LiveKitGateway liveKitGateway;

  @Override
  @Transactional(readOnly = true)
  public MeetingHandRaiseResponse raise(String runtimeSessionId, Long currentUserId) {
    HandRaiseAccess access = requireAccess(runtimeSessionId, currentUserId);
    if (access.member().getRole() != MeetingRole.PARTICIPANT) {
      throw BusinessException.unauthorized(MeetingErrorConstants.HAND_RAISE_NOT_ALLOWED);
    }

    MeetingHandRaiseResponse handRaise = storeRaise(
        access.runtimeSession().getPublicId(),
        new MeetingHandRaiseResponse(
            currentUserId,
            participantIdentity(currentUserId),
            access.user().getFullName(),
            access.member().getRole(),
            Instant.now(),
            true
        )
    );
    broadcast(access.runtimeSession().getProviderRoomName(), handRaise);
    return handRaise;
  }

  @Override
  @Transactional(readOnly = true)
  public void lower(String runtimeSessionId, Long currentUserId) {
    HandRaiseAccess access = requireAccess(runtimeSessionId, currentUserId);
    if (access.member().getRole() != MeetingRole.PARTICIPANT) {
      throw BusinessException.unauthorized(MeetingErrorConstants.HAND_RAISE_NOT_ALLOWED);
    }

    remove(access.runtimeSession().getPublicId(), currentUserId)
        .map(this::lowered)
        .ifPresent(handRaise -> broadcast(
            access.runtimeSession().getProviderRoomName(),
            handRaise
        ));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingHandRaiseResponse> getRaisedHands(
      String runtimeSessionId,
      Long currentUserId
  ) {
    MeetingSession runtimeSession = requireActiveRuntimeSession(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    moderationService.requireNotBanned(runtimeSession.getPublicId(), currentUserId);

    try {
      Set<String> userIds = redisTemplate.opsForZSet().range(
          orderKey(runtimeSession.getPublicId()),
          0,
          -1
      );
      if (userIds == null || userIds.isEmpty()) {
        return List.of();
      }

      List<Object> payloads = redisTemplate.opsForHash().multiGet(
          detailsKey(runtimeSession.getPublicId()),
          new ArrayList<>(userIds)
      );
      List<MeetingHandRaiseResponse> handRaises = new ArrayList<>(payloads.size());
      for (Object payload : payloads) {
        deserialize(payload).ifPresent(handRaises::add);
      }
      return List.copyOf(handRaises);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @Override
  public void removeDisconnected(
      String runtimeSessionId,
      String providerRoomName,
      Long userId
  ) {
    try {
      remove(runtimeSessionId, userId)
          .map(this::lowered)
          .ifPresent(handRaise -> broadcast(providerRoomName, handRaise));
    } catch (BusinessException exception) {
      log.warn(
          "Unable to remove disconnected participant hand raise: runtimeSessionId={}, userId={}",
          runtimeSessionId,
          userId
      );
    }
  }

  @Override
  public void clear(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      return;
    }
    try {
      redisTemplate.delete(List.of(orderKey(runtimeSessionId), detailsKey(runtimeSessionId)));
    } catch (DataAccessException exception) {
      log.warn(
          "Unable to clear meeting hand raises: runtimeSessionId={}",
          runtimeSessionId
      );
    }
  }

  private MeetingHandRaiseResponse storeRaise(
      String runtimeSessionId,
      MeetingHandRaiseResponse handRaise
  ) {
    try {
      String resultJson = redisTemplate.execute(
          RAISE_SCRIPT,
          List.of(orderKey(runtimeSessionId), detailsKey(runtimeSessionId)),
          String.valueOf(handRaise.userId()),
          String.valueOf(handRaise.raisedAt().toEpochMilli()),
          objectMapper.writeValueAsString(handRaise),
          String.valueOf(HAND_RAISE_TTL_SECONDS)
      );
      if (resultJson == null || resultJson.isBlank()) {
        throw unavailable();
      }
      JsonNode result = objectMapper.readTree(resultJson);
      String payload = result.path("payload").asText(null);
      if (payload == null) {
        throw unavailable();
      }
      return objectMapper.readValue(payload, MeetingHandRaiseResponse.class);
    } catch (JsonProcessingException | DataAccessException exception) {
      throw unavailable();
    }
  }

  private Optional<MeetingHandRaiseResponse> remove(
      String runtimeSessionId,
      Long userId
  ) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank() || userId == null) {
      return Optional.empty();
    }
    try {
      String payload = redisTemplate.execute(
          LOWER_SCRIPT,
          List.of(orderKey(runtimeSessionId), detailsKey(runtimeSessionId)),
          String.valueOf(userId)
      );
      return deserialize(payload);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  private Optional<MeetingHandRaiseResponse> deserialize(Object payload) {
    if (payload == null || payload.toString().isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(
          payload.toString(),
          MeetingHandRaiseResponse.class
      ));
    } catch (JsonProcessingException exception) {
      log.warn("Ignoring unreadable meeting hand raise payload");
      return Optional.empty();
    }
  }

  private MeetingHandRaiseResponse lowered(MeetingHandRaiseResponse handRaise) {
    return new MeetingHandRaiseResponse(
        handRaise.userId(),
        handRaise.participantIdentity(),
        handRaise.displayName(),
        handRaise.role(),
        handRaise.raisedAt(),
        false
    );
  }

  private void broadcast(String providerRoomName, MeetingHandRaiseResponse handRaise) {
    try {
      liveKitGateway.broadcastReliableData(
          providerRoomName,
          objectMapper.writeValueAsBytes(handRaise),
          HAND_RAISE_TOPIC
      );
    } catch (JsonProcessingException exception) {
      throw unavailable();
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.PROVIDER_UNAVAILABLE
      );
    }
  }

  private HandRaiseAccess requireAccess(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtimeSession = requireActiveRuntimeSession(runtimeSessionId);
    ClassEnrollment member = authorizationService.requireActiveMember(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    moderationService.requireNotBanned(runtimeSession.getPublicId(), currentUserId);
    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);
    return new HandRaiseAccess(runtimeSession, member, user);
  }

  private MeetingSession requireActiveRuntimeSession(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      throw BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND);
    }
    MeetingSession runtimeSession = runtimeSessionRepository.findByPublicId(runtimeSessionId)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND,
            runtimeSessionId
        ));
    if (runtimeSession.getRuntimeStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSessionId
      );
    }
    return runtimeSession;
  }

  private String orderKey(String runtimeSessionId) {
    return keyPrefix(runtimeSessionId) + ":order";
  }

  private String detailsKey(String runtimeSessionId) {
    return keyPrefix(runtimeSessionId) + ":details";
  }

  private String keyPrefix(String runtimeSessionId) {
    return "meeting:hand-raises:{" + runtimeSessionId + "}";
  }

  private String participantIdentity(Long userId) {
    return "user-" + userId;
  }

  private BusinessException unavailable() {
    return new BusinessException(
        StatusCode.ERROR_IN_BACKEND,
        MeetingErrorConstants.HAND_RAISE_UNAVAILABLE
    );
  }

  private record HandRaiseAccess(
      MeetingSession runtimeSession,
      ClassEnrollment member,
      User user
  ) {
  }
}
