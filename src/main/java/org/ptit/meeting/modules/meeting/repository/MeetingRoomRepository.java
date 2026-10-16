package org.ptit.meeting.modules.meeting.repository;

import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, String> {
}
