package org.ptit.meeting.modules.meeting.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;

public record MeetingRoomSettingUpdateRequest(
    @NotNull(message = "{validation.meeting.setting.participant-mic.required}")
    Boolean participantMicEnabled,

    @NotNull(message = "{validation.meeting.setting.participant-camera.required}")
    Boolean participantCameraEnabled,

    @NotNull(message = "{validation.meeting.setting.participant-screen-share.required}")
    Boolean participantScreenShareEnabled,

    @NotNull(message = "{validation.meeting.setting.max-participants.required}")
    @Min(value = 2, message = "{validation.meeting.setting.max-participants.min}")
    @Max(value = MeetingRoomSetting.MAX_PARTICIPANTS, message = "{validation.meeting.setting.max-participants.max}")
    Integer maxParticipants,

    @NotNull(message = "{validation.meeting.setting.join-before.required}")
    @Min(value = 0, message = "{validation.meeting.setting.join-before.min}")
    @Max(value = 1440, message = "{validation.meeting.setting.join-before.max}")
    Integer joinBeforeMinutes
) {
}
