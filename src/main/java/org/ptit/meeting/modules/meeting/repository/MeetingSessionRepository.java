package org.ptit.meeting.modules.meeting.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSessionRepository extends JpaRepository<MeetingSession, String> {

    Optional<MeetingSession> findByScheduleOccurrenceId(String scheduleOccurrenceId);

    List<MeetingSession> findAllByMeetingRoomIdOrderByScheduledStartAtAsc(String meetingRoomId);
}
