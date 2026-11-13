package org.ptit.meeting.modules.meeting.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRuntimeSessionRepository
    extends JpaRepository<MeetingSession, Long> {

  @EntityGraph(attributePaths = "room")
  Optional<MeetingSession> findByPublicId(String publicId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select session from MeetingSession session where session.publicId = :publicId")
  Optional<MeetingSession> findByPublicIdForUpdate(@Param("publicId") String publicId);

  @EntityGraph(attributePaths = "room")
  Optional<MeetingSession> findByScheduleOccurrenceId(String scheduleOccurrenceId);

  @EntityGraph(attributePaths = "room")
  Optional<MeetingSession> findByProviderRoomName(String providerRoomName);

  List<MeetingSession> findAllByRoomIdAndStatus(Long roomId, SessionStatus status);

  @EntityGraph(attributePaths = "room")
  List<MeetingSession> findAllByStatus(SessionStatus status);

  @EntityGraph(attributePaths = "room")
  List<MeetingSession> findAllByRoom_PublicIdOrderByScheduledStartAsc(
      String meetingRoomId
  );
}
