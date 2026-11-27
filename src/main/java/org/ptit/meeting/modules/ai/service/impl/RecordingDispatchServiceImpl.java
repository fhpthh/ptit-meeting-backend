package org.ptit.meeting.modules.ai.service.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.integration.redis.RecordingDispatchPublisher;
import org.ptit.meeting.modules.ai.service.RecordingDispatchService;
import org.ptit.meeting.modules.ai.service.RecordingDispatchStateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingDispatchServiceImpl implements RecordingDispatchService {
  private final AiRecordingProperties properties;
  private final RecordingDispatchStateService state;
  private final ObjectProvider<RecordingDispatchPublisher> publishers;

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public int dispatch() {
    if (!properties.isEnabled() || !properties.getDispatch().isEnabled()) return 0;
    var publisher = publishers.getIfAvailable();
    if (publisher == null)
      throw new IllegalStateException("Recording dispatch publisher unavailable");
    for (String id : state.expired()) state.recover(id);
    int sent = 0;
    for (var event : state.due()) {
      publisher.publish(
          Map.of(
              "type",
              "recording.dispatch.v1",
              "eventId",
              event.eventId(),
              "jobId",
              event.jobId(),
              "recordingId",
              event.recordingId(),
              "runId",
              event.runId(),
              "recordingEpoch",
              Long.toString(event.recordingEpoch()),
              "dispatchVersion",
              Long.toString(event.dispatchVersion())));
      // Publish/DB ACK gaps intentionally replay the immutable event.
      state.published(event.eventId());
      sent++;
    }
    return sent;
  }
}
