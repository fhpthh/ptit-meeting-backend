package org.ptit.meeting.modules.meeting.event;

public record MeetingSessionEndedEvent(
    String sessionId,
    String providerRoomName
) {
}
