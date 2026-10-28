package org.ptit.meeting.modules.meeting.controller;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.integration.livekit.LiveKitWebhookVerifier;
import org.ptit.meeting.integration.livekit.dto.LiveKitWebhookPayload;
import org.ptit.meeting.modules.meeting.service.MeetingWebhookService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/livekit")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.livekit", name = "enabled", havingValue = "true")
public class LiveKitWebhookController {

  private static final String WEBHOOK_MEDIA_TYPE = "application/webhook+json";

  private final LiveKitWebhookVerifier webhookVerifier;
  private final MeetingWebhookService webhookService;

  @PostMapping(
      value = "/webhook",
      consumes = {WEBHOOK_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE}
  )
  public ResponseEntity<Void> receive(
      @RequestBody byte[] rawBody,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
      String authorizationHeader
  ) {
    LiveKitWebhookPayload payload = webhookVerifier.verify(
        new String(rawBody, StandardCharsets.UTF_8),
        authorizationHeader
    );
    webhookService.process(payload);
    return ResponseEntity.ok().build();
  }
}
