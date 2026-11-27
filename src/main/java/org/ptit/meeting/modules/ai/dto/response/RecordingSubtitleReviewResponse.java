package org.ptit.meeting.modules.ai.dto.response;

import tools.jackson.databind.JsonNode;

public record RecordingSubtitleReviewResponse(
    String draftId, long revision, Long publishedRevision, boolean deleted, JsonNode document) {}
