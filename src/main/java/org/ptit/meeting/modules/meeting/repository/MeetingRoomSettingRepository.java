package org.ptit.meeting.modules.meeting.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomSettingRepository extends JpaRepository<MeetingRoomSetting, String> {

    Optional<MeetingRoomSetting> findByRoomId(String roomId);

    List<MeetingRoomSetting> findAllByRoomIdIn(Collection<String> roomIds);
}
