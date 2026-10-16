package org.ptit.meeting.modules.meeting.repository;

import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomMemberRepository extends JpaRepository<MeetingRoomMember, String> {
}
