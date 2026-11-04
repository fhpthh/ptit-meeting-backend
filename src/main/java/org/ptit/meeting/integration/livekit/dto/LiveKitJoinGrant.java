package org.ptit.meeting.integration.livekit.dto;

public record LiveKitJoinGrant(
    String roomName,
    String participantIdentity,
    String participantName,
    String participantMetadata,
    boolean canSubscribe,
    boolean canPublishMicrophone,
    boolean canPublishCamera,
    boolean canShareScreen
) {
}
