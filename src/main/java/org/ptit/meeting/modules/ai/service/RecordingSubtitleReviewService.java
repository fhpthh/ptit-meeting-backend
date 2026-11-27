package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest.Save;
import org.ptit.meeting.modules.ai.dto.response.RecordingSubtitleReviewResponse;
import tools.jackson.databind.JsonNode;

public interface RecordingSubtitleReviewService {
  RecordingSubtitleReviewResponse get(String recordingId, Long userId);

  RecordingSubtitleReviewResponse loadLatest(
      String recordingId, Long userId, long expectedRevision);

  RecordingSubtitleReviewResponse save(String recordingId, Long userId, Save request);

  RecordingSubtitleReviewResponse publish(String recordingId, Long userId, long expectedRevision);

  void delete(String recordingId, Long userId, long expectedRevision);

  JsonNode published(String recordingId, Long userId);
}
