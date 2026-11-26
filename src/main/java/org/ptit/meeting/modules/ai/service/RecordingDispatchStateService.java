package org.ptit.meeting.modules.ai.service;

import java.util.List;
import org.ptit.meeting.modules.ai.dto.internal.RecordingDispatchSnapshot;

public interface RecordingDispatchStateService {
  List<RecordingDispatchSnapshot> due();

  List<String> expired();

  void published(String eventId);

  boolean recover(String jobId);
}
