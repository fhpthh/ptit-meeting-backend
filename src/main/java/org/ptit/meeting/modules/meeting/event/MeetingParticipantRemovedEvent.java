package org.ptit.meeting.modules.meeting.event;

public record MeetingParticipantRemovedEvent(
    String providerRoomName,
    String participantIdentity
) {
}
