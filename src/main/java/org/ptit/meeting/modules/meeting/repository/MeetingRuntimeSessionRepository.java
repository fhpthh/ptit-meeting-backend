package org.ptit.meeting.modules.meeting.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRuntimeSessionRepository
    extends JpaRepository<MeetingRuntimeSession, String> {

  Optional<MeetingRuntimeSession> findByScheduleOccurrenceId(String scheduleOccurrenceId);

  Optional<MeetingRuntimeSession> findByProviderRoomName(String providerRoomName);

  List<MeetingRuntimeSession> findAllByMeetingRoomIdOrderByScheduledStartAtAsc(
      String meetingRoomId
  );
}
