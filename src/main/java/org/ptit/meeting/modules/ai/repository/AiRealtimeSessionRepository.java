package org.ptit.meeting.modules.ai.repository;

import java.util.Optional;
import org.ptit.meeting.modules.ai.entity.AiRealtimeSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRealtimeSessionRepository extends JpaRepository<AiRealtimeSession, String> {
  Optional<AiRealtimeSession> findByRuntimeSessionId(String runtimeSessionId);
}
