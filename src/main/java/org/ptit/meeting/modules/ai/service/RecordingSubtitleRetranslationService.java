package org.ptit.meeting.modules.ai.service;

import org.ptit.meeting.modules.ai.dto.response.RecordingSubtitleReviewResponse;

public interface RecordingSubtitleRetranslationService {
  RecordingSubtitleReviewResponse translate(
      String recordingId, Long userId, long revision, String segmentId);
}
