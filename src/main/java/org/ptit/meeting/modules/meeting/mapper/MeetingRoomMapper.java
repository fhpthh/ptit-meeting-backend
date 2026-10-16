package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomSettingResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomMapper {

  public MeetingRoomResponse toResponse(MeetingRoom room, MeetingRoomSetting setting) {
    return new MeetingRoomResponse(
        room.getId(),
        room.getCode(),
        room.getName(),
        room.getDescription(),
        room.getRoomType(),
        room.getStatus(),
        room.getCreatedByUserId(),
        toSettingResponse(setting),
        room.getCreatedAt(),
        room.getUpdatedAt()
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
