package org.ptit.meeting.modules.meeting.scheduler;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "application.recording",
    name = "enabled",
    havingValue = "true"
)
public class MeetingRecordingCleanupScheduler {

  private final MeetingRecordingService recordingService;

  @Scheduled(fixedDelayString = "${application.recording.cleanup-interval-ms:120000}")
  public void cleanup() {
    recordingService.cleanupStaleUploads();
  }
}
