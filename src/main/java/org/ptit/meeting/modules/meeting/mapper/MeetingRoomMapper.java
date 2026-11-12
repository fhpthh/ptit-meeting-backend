package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomSettingResponse;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomMapper {

  public MeetingRoomResponse toResponse(Room room, MeetingRoomSetting setting) {
    return new MeetingRoomResponse(
        room.getPublicId(),
        room.getCode(),
        room.getName(),
        room.getDescription(),
        room.getRoomType(),
        room.getStatus(),
        room.getCreatedByUserId(),
        toSettingResponse(setting),
        room.getCreatedAtInstant(),
        room.getUpdatedAtInstant()
    );
  }

  public MeetingRoomSettingResponse toSettingResponse(MeetingRoomSetting setting) {
    return new MeetingRoomSettingResponse(
        setting.isParticipantMicEnabled(),
        setting.isParticipantCameraEnabled(),
        setting.isParticipantScreenShareEnabled(),
        setting.getMaxParticipants(),
        setting.getJoinBeforeMinutes()
    );
  }
}
