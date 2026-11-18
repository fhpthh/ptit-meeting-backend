package org.ptit.meeting.integration.ai;

import org.ptit.meeting.integration.ai.dto.AiRuntimeStartCommand;
import org.ptit.meeting.integration.ai.dto.AiRuntimeView;

public interface AiRuntimeGateway {

  AiRuntimeView start(AiRuntimeStartCommand command);

  AiRuntimeView stop(String runtimeSessionId);

  AiRuntimeView status(String runtimeSessionId);
}
