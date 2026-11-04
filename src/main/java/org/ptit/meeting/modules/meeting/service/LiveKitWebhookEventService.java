package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;

public interface LiveKitWebhookEventService {

  boolean claim(LiveKitWebhookPayload payload);

  void markProcessed(String eventId);

  void markIgnored(String eventId, String reason);

  void markFailed(String eventId, String reason);
}
