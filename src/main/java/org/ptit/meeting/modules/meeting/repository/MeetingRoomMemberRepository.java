package org.ptit.meeting.modules.meeting.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomMemberRepository extends JpaRepository<MeetingRoomMember, String> {

    Optional<MeetingRoomMember> findByRoomIdAndUserId(String roomId, Long userId);

    List<MeetingRoomMember> findAllByRoomIdAndStatusOrderByCreatedAtAsc(
            String roomId,
            MemberStatus status
    );

    List<MeetingRoomMember> findAllByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByRoomIdAndUserId(String roomId, Long userId);
}
