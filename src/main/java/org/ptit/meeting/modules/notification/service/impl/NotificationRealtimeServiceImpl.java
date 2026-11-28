package org.ptit.meeting.modules.notification.service.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.dto.response.NotificationRealtimePayload;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.service.NotificationRealtimeService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRealtimeServiceImpl implements NotificationRealtimeService {

  private static final String EVENT_CONNECTED = "connected";
  private static final String EVENT_NOTIFICATION = "notification";
  private static final String EVENT_HEARTBEAT = "heartbeat";

  private final NotificationMapper notificationMapper;
  private final NotificationProperties properties;

  // Stores active SSE connections per userId (a user may have multiple open browser tabs).
  private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser =
      new ConcurrentHashMap<>();

  @Override
  public SseEmitter subscribe(Long userId) {
    SseEmitter emitter = createEmitter(properties.getSseTimeoutMs());
    if (!properties.isSseEnabled() || userId == null) {
      emitter.complete();
      return emitter;
    }

    CopyOnWriteArrayList<SseEmitter> userEmitters = emittersByUser
        .computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());

    // Evict oldest tab connection when user exceeds configured per-user limit.
    int maxConnections = Math.max(1, properties.getMaxSseConnectionsPerUser());
    while (userEmitters.size() >= maxConnections) {
      SseEmitter oldest = userEmitters.removeFirst();
      completeQuietly(oldest);
    }

    userEmitters.add(emitter);
    registerLifecycleCallbacks(userId, emitter);

    // Confirm stream establishment immediately so proxies flush headers.
    sendEvent(userId, emitter, SseEmitter.event()
        .name(EVENT_CONNECTED)
        .data(Map.of("timestamp", Instant.now().toString())));

    return emitter;
  }

  @Override
  public void pushToUsers(Notification notification, List<NotificationRecipient> recipients) {
    if (!properties.isSseEnabled()
        || notification == null
        || recipients == null
        || recipients.isEmpty()) {
      return;
    }

    for (NotificationRecipient recipient : recipients) {
      CopyOnWriteArrayList<SseEmitter> userEmitters = emittersByUser.get(recipient.getUserId());
      if (userEmitters == null || userEmitters.isEmpty()) {
        // User has no active SSE connection; skip in O(1).
        continue;
      }

      NotificationRealtimePayload payload = notificationMapper
          .toRealtimePayload(notification, recipient);
      for (SseEmitter emitter : userEmitters) {
        sendEvent(recipient.getUserId(), emitter, SseEmitter.event()
            .id(recipient.getId())
            .name(EVENT_NOTIFICATION)
            .data(payload));
      }
    }
  }

  @Override
  public void sendHeartbeat() {
    if (!properties.isSseEnabled() || emittersByUser.isEmpty()) {
      return;
    }
    String timestamp = Instant.now().toString();
    emittersByUser.forEach((userId, emitters) -> {
      for (SseEmitter emitter : emitters) {
        sendEvent(userId, emitter, SseEmitter.event()
            .name(EVENT_HEARTBEAT)
            .data(timestamp));
      }
    });
  }

  @Override
  public int getActiveConnectionCount() {
    return emittersByUser.values().stream()
        .mapToInt(List::size)
        .sum();
  }

  /** Factory method extracted for clean unit testing of SseEmitter behavior. */
  protected SseEmitter createEmitter(long timeoutMs) {
    return new SseEmitter(timeoutMs);
  }

  private void registerLifecycleCallbacks(Long userId, SseEmitter emitter) {
    emitter.onCompletion(() -> removeEmitter(userId, emitter));
    emitter.onTimeout(() -> {
      removeEmitter(userId, emitter);
      completeQuietly(emitter);
    });
    emitter.onError(ex -> removeEmitter(userId, emitter));
  }

  private void sendEvent(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
    try {
      emitter.send(event);
    } catch (IOException | IllegalStateException ex) {
      log.debug("Removing closed SSE emitter for user {}: {}", userId, ex.getMessage());
      removeEmitter(userId, emitter);
      completeQuietly(emitter);
    }
  }

  private void removeEmitter(Long userId, SseEmitter emitter) {
    emittersByUser.computeIfPresent(userId, (key, list) -> {
      list.remove(emitter);
      return list.isEmpty() ? null : list;
    });
  }

  private void completeQuietly(SseEmitter emitter) {
    try {
      emitter.complete();
    } catch (Exception ignored) {
      // Emitter may already be completed or disconnected.
    }
  }
}
