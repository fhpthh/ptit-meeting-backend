package org.ptit.meeting.modules.meeting.dto.response;

public record MeetingRoomSettingResponse(
    boolean participantMicEnabled,
    boolean participantCameraEnabled,
    boolean participantScreenShareEnabled,
    int maxParticipants,
    int joinBeforeMinutes
) {
}
