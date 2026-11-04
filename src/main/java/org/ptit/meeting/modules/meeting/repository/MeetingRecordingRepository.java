package org.ptit.meeting.modules.meeting.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRecordingStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, String> {

  List<MeetingRecording> findAllByRuntimeSessionIdOrderByCreatedAtDesc(
      String runtimeSessionId
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MeetingRecording> findByActiveSlot(String runtimeSessionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select recording from MeetingRecording recording where recording.id = :id")
  Optional<MeetingRecording> findByIdForUpdate(@Param("id") String id);

  List<MeetingRecording> findTop50ByStatusInAndLastActivityAtBeforeOrderByLastActivityAtAsc(
      Collection<MeetingRecordingStatus> statuses,
      Instant activityBefore
  );
}
