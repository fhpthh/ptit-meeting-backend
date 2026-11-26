package org.ptit.meeting.modules.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "application.ai-recording.dispatch",
    name = "enabled",
    havingValue = "true")
public class RecordingDispatchScheduler {
  private final RecordingDispatchService dispatcher;

  @Scheduled(fixedDelayString = "${application.ai-recording.dispatch.poll-interval-ms:5000}")
  public void poll() {
    dispatcher.dispatch();
  }
}
