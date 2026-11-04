package org.ptit.meeting.modules.meeting.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, String> {

    Optional<MeetingRoom> findByCode(String code);

    boolean existsByCode(String code);

    List<MeetingRoom> findAllByIdInAndStatusOrderByCreatedAtDesc(
            Collection<String> ids,
            RoomStatus status
    );
}
