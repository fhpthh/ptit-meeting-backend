package org.ptit.meeting.modules.meeting.repository;

import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSessionRepository extends JpaRepository<MeetingSession, String> {
}
