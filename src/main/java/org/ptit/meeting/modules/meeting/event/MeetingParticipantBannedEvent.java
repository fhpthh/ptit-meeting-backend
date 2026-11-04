package org.ptit.meeting.modules.meeting.event;

public record MeetingParticipantBannedEvent(
    String providerRoomName,
    String participantIdentity
) {
}
