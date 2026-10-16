package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;

public record MeetingRoomCreateRequest(
    @NotBlank(message = "{validation.meeting.room.name.required}")
    @Size(max = 150, message = "{validation.meeting.room.name.size}")
    String name,

    @Size(max = 500, message = "{validation.meeting.room.description.size}")
    String description,

    @NotNull(message = "{validation.meeting.room.type.required}")
    RoomType roomType
) {
}
