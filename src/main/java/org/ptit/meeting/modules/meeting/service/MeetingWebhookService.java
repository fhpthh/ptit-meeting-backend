package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;

public interface MeetingWebhookService {

  void process(LiveKitWebhookPayload payload);
}
