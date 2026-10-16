package org.ptit.meeting.modules.meeting.repository;

import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomSettingRepository extends JpaRepository<MeetingRoomSetting, String> {

}
