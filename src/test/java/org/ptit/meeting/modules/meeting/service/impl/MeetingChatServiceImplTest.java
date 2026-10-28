package org.ptit.meeting.modules.meeting.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.LiveKitIntegrationException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;
import org.ptit.meeting.modules.meeting.dto.request.MeetingChatMessageRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingChatMessageResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class MeetingChatServiceImplTest {

  private static final String SESSION_ID = "session-1";
  private static final String ROOM_ID = "room-1";
  private static final String PROVIDER_ROOM = "provider-room-1";
  private static final Long USER_ID = 10L;

  @Mock
  private MeetingRuntimeSessionRepository runtimeSessionRepository;

  @Mock
  private MeetingAuthorizationService authorizationService;

  @Mock
  private MeetingModerationService moderationService;

  @Mock
  private UserService userService;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private StreamOperations<String, Object, Object> streamOperations;

  @Mock
  private LiveKitGateway liveKitGateway;

  private ObjectMapper objectMapper;
  private MeetingChatServiceImpl chatService;
  private MeetingRuntimeSession runtimeSession;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    chatService = new MeetingChatServiceImpl(
        runtimeSessionRepository,
        authorizationService,
        moderationService,
        userService,
        redisTemplate,
        objectMapper,
        liveKitGateway
    );

    runtimeSession = new MeetingRuntimeSession(
        ROOM_ID,
        "Session",
        PROVIDER_ROOM,
        null,
        Instant.now().minusSeconds(60),
        Instant.now().plusSeconds(3600),
        USER_ID
    );
    runtimeSession.setId(SESSION_ID);
    runtimeSession.setStatus(MeetingRuntimeSessionStatus.ACTIVE);
  }

  @Test
  void sendMessageStoresAndBroadcastsNormalizedMessage() throws Exception {
    prepareActiveAccess();
    mockSuccessfulAppend("1000-0");
    UUID clientMessageId = UUID.randomUUID();

    MeetingChatMessageResponse response = chatService.sendMessage(
        SESSION_ID,
        new MeetingChatMessageRequest(clientMessageId, "  Hello class  "),
        USER_ID
    );

    assertEquals(clientMessageId, response.clientMessageId());
    assertEquals("1000-0", response.cursor());
    assertEquals("Hello class", response.content());
    assertEquals("user-10", response.senderIdentity());
    assertEquals(MeetingRole.PARTICIPANT, response.senderRole());

    ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(liveKitGateway).broadcastReliableData(
        org.mockito.ArgumentMatchers.eq(PROVIDER_ROOM),
        payloadCaptor.capture(),
        org.mockito.ArgumentMatchers.eq("meeting.chat.message.v1")
    );
    MeetingChatMessageResponse broadcast = objectMapper.readValue(
        payloadCaptor.getValue(),
        MeetingChatMessageResponse.class
    );
    assertEquals(response, broadcast);
  }

  @Test
  void sendMessageRejectsInactiveRuntimeSession() {
    runtimeSession.setStatus(MeetingRuntimeSessionStatus.ENDED);
    when(runtimeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runtimeSession));
    when(authorizationService.requireActiveMember(ROOM_ID, USER_ID))
        .thenReturn(new MeetingRoomMember(ROOM_ID, USER_ID, MeetingRole.PARTICIPANT));

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> chatService.sendMessage(
            SESSION_ID,
            new MeetingChatMessageRequest(UUID.randomUUID(), "Hello"),
            USER_ID
        )
    );

    assertEquals(MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE, exception.getStrCode());
    verify(redisTemplate, never()).execute(
        org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
        anyList(),
        any(Object[].class)
    );
  }

  @Test
  void sendMessageReturnsRateLimitBusinessError() {
    prepareActiveAccess();
    when(redisTemplate.execute(
        org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
        anyList(),
        any(Object[].class)
    )).thenReturn("{\"status\":\"RATE_LIMITED\"}");

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> chatService.sendMessage(
            SESSION_ID,
            new MeetingChatMessageRequest(UUID.randomUUID(), "Hello"),
            USER_ID
        )
    );

    assertEquals(StatusCode.RATE_LIMIT_EXCEEDED, exception.getStatusCode());
    assertEquals(MeetingErrorConstants.CHAT_RATE_LIMIT_EXCEEDED, exception.getStrCode());
    verify(liveKitGateway, never()).broadcastReliableData(anyString(), any(), anyString());
  }

  @Test
  void sendMessageMapsLiveKitFailureToProviderUnavailable() throws Exception {
    prepareActiveAccess();
    mockSuccessfulAppend("1000-0");
    org.mockito.Mockito.doThrow(new LiveKitIntegrationException("unavailable"))
        .when(liveKitGateway)
        .broadcastReliableData(anyString(), any(), anyString());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> chatService.sendMessage(
            SESSION_ID,
            new MeetingChatMessageRequest(UUID.randomUUID(), "Hello"),
            USER_ID
        )
    );

    assertEquals(MeetingErrorConstants.PROVIDER_UNAVAILABLE, exception.getStrCode());
  }

  @Test
  void getMessagesReturnsChronologicalPage() throws Exception {
    prepareActiveAccess();
    when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    MeetingChatMessageResponse older = message("older", "1000-0");
    MeetingChatMessageResponse newer = message("newer", "1001-0");
    List<MapRecord<String, Object, Object>> newestFirst = List.of(
        record(newer, "1001-0"),
        record(older, "1000-0")
    );
    when(streamOperations.reverseRange(
        anyString(),
        org.mockito.ArgumentMatchers.<Range<String>>any(),
        any(Limit.class)
    )).thenReturn(newestFirst);

    List<MeetingChatMessageResponse> messages = chatService.getMessages(
        SESSION_ID,
        "1002-0",
        50,
        USER_ID
    );

    assertEquals(List.of("older", "newer"), messages.stream()
        .map(MeetingChatMessageResponse::content)
        .toList());
    assertEquals(List.of("1000-0", "1001-0"), messages.stream()
        .map(MeetingChatMessageResponse::cursor)
        .toList());
  }

  @Test
  void getMessagesRejectsMalformedCursor() {
    prepareActiveAccess();

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> chatService.getMessages(SESSION_ID, "not-a-cursor", 50, USER_ID)
    );

    assertEquals(MeetingErrorConstants.CHAT_CURSOR_INVALID, exception.getStrCode());
    verify(redisTemplate, never()).opsForStream();
  }

  @Test
  void clearHistoryDeletesSessionStreamAndIdempotencyKeys() {
    chatService.clearHistory(SESSION_ID);

    verify(redisTemplate).delete(List.of(
        "meeting:chat:{session-1}:messages",
        "meeting:chat:{session-1}:idempotency"
    ));
  }

  private void prepareActiveAccess() {
    when(runtimeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runtimeSession));
    when(authorizationService.requireActiveMember(ROOM_ID, USER_ID))
        .thenReturn(new MeetingRoomMember(ROOM_ID, USER_ID, MeetingRole.PARTICIPANT));
    User user = User.builder().id(USER_ID).fullName("Student A").build();
    when(userService.getUserById(USER_ID)).thenReturn(user);
  }

  private void mockSuccessfulAppend(String cursor) {
    when(redisTemplate.execute(
        org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
        anyList(),
        any(Object[].class)
    )).thenAnswer(invocation -> {
      ObjectNode result = objectMapper.createObjectNode();
      result.put("status", "OK");
      result.put("cursor", cursor);
      result.put("payload", invocation.getArgument(5, String.class));
      return objectMapper.writeValueAsString(result);
    });
  }

  private MeetingChatMessageResponse message(String content, String cursor) {
    return new MeetingChatMessageResponse(
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        cursor,
        USER_ID,
        "user-10",
        "Student A",
        MeetingRole.PARTICIPANT,
        content,
        Instant.now()
    );
  }

  private MapRecord<String, Object, Object> record(
      MeetingChatMessageResponse message,
      String cursor
  ) throws Exception {
    MeetingChatMessageResponse stored = new MeetingChatMessageResponse(
        message.messageId(),
        message.clientMessageId(),
        null,
        message.senderUserId(),
        message.senderIdentity(),
        message.senderName(),
        message.senderRole(),
        message.content(),
        message.sentAt()
    );
    return MapRecord.create(
        "meeting:chat:{session-1}:messages",
        java.util.Map.<Object, Object>of("payload", objectMapper.writeValueAsString(stored))
    ).withId(RecordId.of(cursor));
  }
}
