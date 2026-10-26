package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;

public interface LiveKitWebhookVerifier {

  LiveKitWebhookPayload verify(String rawBody, String authorizationHeader);
}
