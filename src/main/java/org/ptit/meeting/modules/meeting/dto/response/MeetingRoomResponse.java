package org.ptit.meeting.modules.meeting.dto.response;

import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;

import java.time.Instant;

public record MeetingRoomResponse(
    String id,
    String code,
    String name,
    String description,
    RoomType roomType,
    RoomStatus status,
    Long createdByUserId,
    MeetingRoomSettingResponse setting,
    Instant createdAt,
    Instant updatedAt
) {
}
