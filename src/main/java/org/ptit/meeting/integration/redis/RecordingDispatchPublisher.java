package org.ptit.meeting.integration.redis;

import java.util.Map;

public interface RecordingDispatchPublisher {
  String publish(Map<String, String> fields);
}
