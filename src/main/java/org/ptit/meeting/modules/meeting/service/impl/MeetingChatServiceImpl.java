package org.ptit.meeting.modules.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingChatMessageRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingChatMessageResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingChatService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingChatServiceImpl implements MeetingChatService {

  private static final String CHAT_TOPIC = "meeting.chat.message.v1";
  private static final String PAYLOAD_FIELD = "payload";
  private static final String APPEND_STATUS_OK = "OK";
  private static final String APPEND_STATUS_RATE_LIMITED = "RATE_LIMITED";
  private static final int MESSAGE_MAX_LENGTH = 1000;
  private static final int HISTORY_DEFAULT_LIMIT = 50;
  private static final int HISTORY_MAX_LIMIT = 100;
  private static final int STREAM_MAX_MESSAGES = 5000;
  private static final long CHAT_TTL_SECONDS = 24 * 60 * 60L;
  private static final int USER_RATE_LIMIT = 5;
  private static final int USER_RATE_WINDOW_SECONDS = 10;
  private static final int ROOM_RATE_LIMIT = 20;
  private static final int ROOM_RATE_WINDOW_SECONDS = 1;
  private static final Pattern STREAM_CURSOR_PATTERN = Pattern.compile("^[0-9]+-[0-9]+$");

  private static final DefaultRedisScript<String> APPEND_MESSAGE_SCRIPT =
      new DefaultRedisScript<>("""
          local existing = redis.call('HGET', KEYS[2], ARGV[3])
          if existing then
            local entry = redis.call('XRANGE', KEYS[1], existing, existing, 'COUNT', 1)
            if #entry > 0 then
              return cjson.encode({
                status = 'OK',
                cursor = existing,
                payload = entry[1][2][2]
              })
            end
            redis.call('HDEL', KEYS[2], ARGV[3])
          end

          local userCount = redis.call('INCR', KEYS[3])
          if userCount == 1 then
            redis.call('EXPIRE', KEYS[3], ARGV[6])
          end
          if userCount > tonumber(ARGV[5]) then
            return cjson.encode({status = 'RATE_LIMITED'})
          end

          local roomCount = redis.call('INCR', KEYS[4])
          if roomCount == 1 then
            redis.call('EXPIRE', KEYS[4], ARGV[8])
          end
          if roomCount > tonumber(ARGV[7]) then
            return cjson.encode({status = 'RATE_LIMITED'})
          end

          local cursor = redis.call(
            'XADD', KEYS[1], 'MAXLEN', '~', ARGV[1], '*', 'payload', ARGV[4]
          )
          redis.call('HSET', KEYS[2], ARGV[3], cursor)
          redis.call('EXPIRE', KEYS[1], ARGV[2])
          redis.call('EXPIRE', KEYS[2], ARGV[2])

          return cjson.encode({
            status = 'OK',
            cursor = cursor,
            payload = ARGV[4]
          })
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
  public MeetingChatMessageResponse sendMessage(
      String runtimeSessionId,
      MeetingChatMessageRequest request,
      Long currentUserId
  ) {
    ChatAccess access = requireChatAccess(runtimeSessionId, currentUserId);
    String content = normalizeContent(request);

    MeetingChatMessageResponse pendingMessage = new MeetingChatMessageResponse(
        UUID.randomUUID().toString(),
        request.clientMessageId(),
        null,
        currentUserId,
        participantIdentity(currentUserId),
        access.user().getFullName(),
        access.member().getRole(),
        content,
        Instant.now()
    );
    MeetingChatMessageResponse storedMessage = appendMessage(
        access.runtimeSession().getId(),
        pendingMessage
    );
    broadcast(access.runtimeSession().getProviderRoomName(), storedMessage);
    return storedMessage;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingChatMessageResponse> getMessages(
      String runtimeSessionId,
      String before,
      int limit,
      Long currentUserId
  ) {
    MeetingRuntimeSession runtimeSession = requireChatAccess(
        runtimeSessionId,
        currentUserId
    ).runtimeSession();
    String normalizedCursor = normalizeCursor(before);
    int normalizedLimit = normalizeHistoryLimit(limit);
    Range<String> range = normalizedCursor == null
        ? Range.unbounded()
        : Range.leftUnbounded(Range.Bound.exclusive(normalizedCursor));

    try {
      List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
          .reverseRange(
              streamKey(runtimeSession.getId()),
              range,
              Limit.limit().count(normalizedLimit)
          );
      if (records == null || records.isEmpty()) {
        return List.of();
      }

      List<MeetingChatMessageResponse> messages = new ArrayList<>(records.size());
      for (MapRecord<String, Object, Object> record : records) {
        deserializeRecord(record).ifPresent(messages::add);
      }
      Collections.reverse(messages);
      return List.copyOf(messages);
    } catch (DataAccessException exception) {
      throw chatUnavailable();
    }
  }

  @Override
  public void clearHistory(String runtimeSessionId) {
    if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
      return;
    }
    try {
      redisTemplate.delete(List.of(
          streamKey(runtimeSessionId),
          idempotencyKey(runtimeSessionId)
      ));
    } catch (DataAccessException exception) {
      log.warn(
          "Unable to clear temporary meeting chat history: runtimeSessionId={}",
          runtimeSessionId
      );
    }
  }

  private MeetingChatMessageResponse appendMessage(
      String runtimeSessionId,
      MeetingChatMessageResponse message
  ) {
    try {
      String payload = objectMapper.writeValueAsString(message);
      String resultJson = redisTemplate.execute(
          APPEND_MESSAGE_SCRIPT,
          List.of(
              streamKey(runtimeSessionId),
              idempotencyKey(runtimeSessionId),
              userRateKey(runtimeSessionId, message.senderUserId()),
              roomRateKey(runtimeSessionId)
          ),
          String.valueOf(STREAM_MAX_MESSAGES),
          String.valueOf(CHAT_TTL_SECONDS),
          idempotencyField(message.senderUserId(), message.clientMessageId()),
          payload,
          String.valueOf(USER_RATE_LIMIT),
          String.valueOf(USER_RATE_WINDOW_SECONDS),
          String.valueOf(ROOM_RATE_LIMIT),
          String.valueOf(ROOM_RATE_WINDOW_SECONDS)
      );
      if (resultJson == null || resultJson.isBlank()) {
        throw chatUnavailable();
      }

      JsonNode result = objectMapper.readTree(resultJson);
      if (APPEND_STATUS_RATE_LIMITED.equals(result.path("status").asText())) {
        throw new BusinessException(
            StatusCode.RATE_LIMIT_EXCEEDED,
            MeetingErrorConstants.CHAT_RATE_LIMIT_EXCEEDED
        );
      }
      if (!APPEND_STATUS_OK.equals(result.path("status").asText())) {
        throw chatUnavailable();
      }

      String cursor = result.path("cursor").asText(null);
      String storedPayload = result.path("payload").asText(null);
      if (cursor == null || storedPayload == null) {
        throw chatUnavailable();
      }
      return withCursor(
          objectMapper.readValue(storedPayload, MeetingChatMessageResponse.class),
          cursor
      );
    } catch (JsonProcessingException | DataAccessException exception) {
      throw chatUnavailable();
    }
  }

  private Optional<MeetingChatMessageResponse> deserializeRecord(
      MapRecord<String, Object, Object> record
  ) {
    Object payload = record.getValue().get(PAYLOAD_FIELD);
    if (payload == null) {
      log.warn("Ignoring malformed meeting chat record: cursor={}", record.getId());
      return Optional.empty();
    }

    try {
      MeetingChatMessageResponse message = objectMapper.readValue(
          payload.toString(),
          MeetingChatMessageResponse.class
      );
      return Optional.of(withCursor(message, record.getId().getValue()));
    } catch (JsonProcessingException exception) {
      log.warn("Ignoring unreadable meeting chat record: cursor={}", record.getId());
      return Optional.empty();
    }
  }

  private void broadcast(String providerRoomName, MeetingChatMessageResponse message) {
    try {
      liveKitGateway.broadcastReliableData(
          providerRoomName,
          objectMapper.writeValueAsBytes(message),
          CHAT_TOPIC
      );
    } catch (JsonProcessingException exception) {
      throw chatUnavailable();
    } catch (LiveKitIntegrationException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND,
          MeetingErrorConstants.PROVIDER_UNAVAILABLE
      );
    }
  }

  private ChatAccess requireChatAccess(String runtimeSessionId, Long currentUserId) {
    MeetingRuntimeSession runtimeSession = findRuntimeSession(runtimeSessionId);
    MeetingRoomMember member = authorizationService.requireActiveMember(
        runtimeSession.getMeetingRoomId(),
        currentUserId
    );
    if (runtimeSession.getStatus() != MeetingRuntimeSessionStatus.ACTIVE) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE,
          runtimeSessionId
      );
    }
    moderationService.requireNotBanned(runtimeSession.getId(), currentUserId);
    User user = userService.getUserById(currentUserId);
    userService.validateUserStatus(user);
    return new ChatAccess(runtimeSession, member, user);
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

  private String normalizeContent(MeetingChatMessageRequest request) {
    if (request == null
        || request.clientMessageId() == null
        || request.content() == null
        || request.content().isBlank()
        || request.content().length() > MESSAGE_MAX_LENGTH) {
      throw BusinessException.invalidInput(MeetingErrorConstants.CHAT_MESSAGE_INVALID);
    }

    String content = request.content().strip();
    boolean containsUnsupportedControlCharacter = content.codePoints().anyMatch(
        character -> Character.isISOControl(character)
            && character != '\n'
            && character != '\t'
    );
    if (content.isEmpty() || containsUnsupportedControlCharacter) {
      throw BusinessException.invalidInput(MeetingErrorConstants.CHAT_MESSAGE_INVALID);
    }
    return content;
  }

  private String normalizeCursor(String cursor) {
    if (cursor == null) {
      return null;
    }
    String normalized = cursor.trim();
    if (!STREAM_CURSOR_PATTERN.matcher(normalized).matches()) {
      throw BusinessException.invalidInput(MeetingErrorConstants.CHAT_CURSOR_INVALID);
    }
    return normalized;
  }

  private int normalizeHistoryLimit(int limit) {
    if (limit == 0) {
      return HISTORY_DEFAULT_LIMIT;
    }
    if (limit < 1 || limit > HISTORY_MAX_LIMIT) {
      throw BusinessException.invalidInput(
          MeetingErrorConstants.CHAT_HISTORY_LIMIT_INVALID,
          HISTORY_MAX_LIMIT
      );
    }
    return limit;
  }

  private MeetingChatMessageResponse withCursor(
      MeetingChatMessageResponse message,
      String cursor
  ) {
    return new MeetingChatMessageResponse(
        message.messageId(),
        message.clientMessageId(),
        cursor,
        message.senderUserId(),
        message.senderIdentity(),
        message.senderName(),
        message.senderRole(),
        message.content(),
        message.sentAt()
    );
  }

  private String streamKey(String runtimeSessionId) {
    return keyPrefix(runtimeSessionId) + ":messages";
  }

  private String idempotencyKey(String runtimeSessionId) {
    return keyPrefix(runtimeSessionId) + ":idempotency";
  }

  private String userRateKey(String runtimeSessionId, Long userId) {
    return keyPrefix(runtimeSessionId) + ":rate:user:" + userId;
  }

  private String roomRateKey(String runtimeSessionId) {
    return keyPrefix(runtimeSessionId) + ":rate:room";
  }

  private String keyPrefix(String runtimeSessionId) {
    return "meeting:chat:{" + runtimeSessionId + "}";
  }

  private String idempotencyField(Long userId, UUID clientMessageId) {
    return userId + ":" + clientMessageId;
  }

  private String participantIdentity(Long userId) {
    return "user-" + userId;
  }

  private BusinessException chatUnavailable() {
    return new BusinessException(
        StatusCode.ERROR_IN_BACKEND,
        MeetingErrorConstants.CHAT_UNAVAILABLE
    );
  }

  private record ChatAccess(
      MeetingRuntimeSession runtimeSession,
      MeetingRoomMember member,
      User user
  ) {
  }
}
