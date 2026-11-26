package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.internal.RecordingTrustedWorker;
import org.ptit.meeting.modules.ai.dto.request.RecordingProvisionRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;

public interface RecordingWorkerProvisioningService {
  RecordingWorkerGrantResponse claim(
      RecordingTrustedWorker worker, RecordingProvisionRequest request);
}
