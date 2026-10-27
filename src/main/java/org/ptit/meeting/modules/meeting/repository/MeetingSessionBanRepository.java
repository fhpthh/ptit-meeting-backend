package org.ptit.meeting.modules.meeting.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.MeetingSessionBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MeetingSessionBanRepository extends JpaRepository<MeetingSessionBan, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MeetingSessionBan> findByRuntimeSessionIdAndUserId(
      String runtimeSessionId,
      Long userId
  );

  boolean existsByRuntimeSessionIdAndUserIdAndRevokedAtIsNull(
      String runtimeSessionId,
      Long userId
  );

  List<MeetingSessionBan> findAllByRuntimeSessionIdAndRevokedAtIsNullOrderByBannedAtDesc(
      String runtimeSessionId
  );
}
