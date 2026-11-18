package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.integration.livekit.dto.LiveKitParticipantSnapshot;

public interface AiWorkerAuthorizationService {

  boolean isAuthorized(String runtimeSessionId, LiveKitParticipantSnapshot participant);
}
