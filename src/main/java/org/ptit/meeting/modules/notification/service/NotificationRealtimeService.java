package org.ptit.meeting.modules.notification.service;

import java.util.List;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages Server-Sent Events (SSE) subscriptions and realtime notification pushes.
 */
public interface NotificationRealtimeService {

  /** Opens a new SSE stream for the authenticated user. */
  SseEmitter subscribe(Long userId);

  /** Pushes a newly published notification to all currently connected recipients. */
  void pushToUsers(Notification notification, List<NotificationRecipient> recipients);

  /** Sends a lightweight heartbeat event to keep active SSE connections alive. */
  void sendHeartbeat();

  /** Returns the total number of active SSE connections across all users. */
  int getActiveConnectionCount();
}
