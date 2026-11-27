package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;

public interface RecordingWorkerTokenService {
  String issue(RecordingWorkerIdentity identity);

  RecordingWorkerIdentity verify(String token);
}
