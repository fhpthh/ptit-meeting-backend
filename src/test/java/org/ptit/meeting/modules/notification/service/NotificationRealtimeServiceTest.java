package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.service.impl.NotificationRealtimeServiceImpl;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationRealtimeServiceTest {

  private NotificationProperties properties;
  private Queue<SseEmitter> mockEmitters;
  private NotificationRealtimeServiceImpl realtimeService;

  @BeforeEach
  void setUp() {
    properties = new NotificationProperties();
    properties.setSseEnabled(true);
    properties.setSseTimeoutMs(60_000L);
    properties.setMaxSseConnectionsPerUser(2);
    mockEmitters = new ArrayDeque<>();

    realtimeService = new NotificationRealtimeServiceImpl(new NotificationMapper(), properties) {
      @Override
      protected SseEmitter createEmitter(long timeoutMs) {
        return mockEmitters.isEmpty() ? mock(SseEmitter.class) : mockEmitters.poll();
      }
    };
  }

  @Test
  void subscribe_whenExceedingMaxConnectionsPerUser_evictsOldestEmitter() {
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    SseEmitter emitter3 = mock(SseEmitter.class);
    mockEmitters.addAll(List.of(emitter1, emitter2, emitter3));

    realtimeService.subscribe(10L);
    realtimeService.subscribe(10L);
    realtimeService.subscribe(10L);

    assertEquals(2, realtimeService.getActiveConnectionCount());
    verify(emitter1).complete();
  }

  @Test
  void pushToUsers_sendsOnlyToConnectedUsersAndRemovesFailedEmitter() throws Exception {
    SseEmitter healthyEmitter = mock(SseEmitter.class);
    SseEmitter brokenEmitter = mock(SseEmitter.class);
    mockEmitters.addAll(List.of(healthyEmitter, brokenEmitter));

    realtimeService.subscribe(10L);
    realtimeService.subscribe(20L);

    doThrow(new IOException("Client disconnected"))
        .when(brokenEmitter).send(any(SseEmitter.SseEventBuilder.class));

    Notification notification = Notification.builder()
        .type(NotificationType.PGV_ANNOUNCEMENT)
        .category(NotificationCategory.ACADEMIC)
        .priority(NotificationPriority.HIGH)
        .title("Exam notice")
        .content("Starts tomorrow")
        .publishedAt(Instant.now())
        .build();
    notification.setId("noti-1");

    NotificationRecipient rec10 = NotificationRecipient.builder()
        .notificationId("noti-1").userId(10L).build();
    rec10.setId("rec-10");
    NotificationRecipient rec20 = NotificationRecipient.builder()
        .notificationId("noti-1").userId(20L).build();
    rec20.setId("rec-20");
    NotificationRecipient recOffline = NotificationRecipient.builder()
        .notificationId("noti-1").userId(30L).build();
    recOffline.setId("rec-30");

    realtimeService.pushToUsers(notification, List.of(rec10, rec20, recOffline));

    // User 10 received initial connected + notification event (2 sends total)
    verify(healthyEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    // User 20's broken emitter was evicted and completed
    verify(brokenEmitter).complete();
    assertEquals(1, realtimeService.getActiveConnectionCount());
  }

  @Test
  void sendHeartbeat_sendsHeartbeatToAllActiveConnections() throws Exception {
    SseEmitter emitter = mock(SseEmitter.class);
    mockEmitters.add(emitter);
    realtimeService.subscribe(10L);

    realtimeService.sendHeartbeat();

    // 1 connected event + 1 heartbeat event
    verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
  }
}
