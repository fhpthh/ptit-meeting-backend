package org.ptit.meeting.modules.ai.dto.request;

public record AiFinalCaptionRequest(
    Integer schemaVersion,
    String sessionId,
    String streamId,
    Long generation,
    String segmentId,
    String language,
    Long revision,
    String status,
    String text
) {
}
