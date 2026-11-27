package org.ptit.meeting.modules.ai.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record RecordingSubtitleReviewResponse(
    String draftId, long revision, Long publishedRevision, boolean deleted, JsonNode document) {}
