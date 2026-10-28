package org.ptit.meeting.modules.meeting.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.integration.livekit.LiveKitWebhookVerifier;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.service.MeetingWebhookService;

@ExtendWith(MockitoExtension.class)
class LiveKitWebhookControllerTest {

  @Mock
  private LiveKitWebhookVerifier webhookVerifier;

  @Mock
  private MeetingWebhookService webhookService;

  private LiveKitWebhookController controller;

  @BeforeEach
  void setUp() {
    controller = new LiveKitWebhookController(webhookVerifier, webhookService);
  }

  @Test
  void receivePreservesUtf8WebhookBodyForSignatureVerification() {
    String rawBody = """
        {"event":"participant_joined","participant":{"name":"Nguyễn Văn An"}}
        """.strip();
    String authorizationHeader = "signed-webhook-token";
    LiveKitWebhookPayload payload = new LiveKitWebhookPayload(
        "event-1",
        "participant_joined",
        Instant.parse("2026-10-28T04:19:44Z"),
        "room-1",
        "participant-1",
        "user-1",
        "{\"fullName\":\"Nguyễn Văn An\"}",
        null
    );
    when(webhookVerifier.verify(rawBody, authorizationHeader)).thenReturn(payload);

    var response = controller.receive(
        rawBody.getBytes(StandardCharsets.UTF_8),
        authorizationHeader
    );

    assertEquals(200, response.getStatusCode().value());
    verify(webhookVerifier).verify(rawBody, authorizationHeader);
    verify(webhookService).process(payload);
  }
}
