package org.ptit.meeting.modules.meeting.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.ParticipantConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantConnectionRepository
    extends JpaRepository<ParticipantConnection, String> {

  Optional<ParticipantConnection> findByRuntimeSessionIdAndParticipantSid(
      String runtimeSessionId,
      String participantSid
  );

  List<ParticipantConnection> findAllByRuntimeSessionId(String runtimeSessionId);
}
