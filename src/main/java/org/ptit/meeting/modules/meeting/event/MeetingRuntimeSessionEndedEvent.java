package org.ptit.meeting.modules.meeting.event;

public record MeetingRuntimeSessionEndedEvent(
    String runtimeSessionId,
    String providerRoomName
) {
}
