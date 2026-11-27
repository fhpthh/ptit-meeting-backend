package org.ptit.meeting.modules.ai.repository;

import java.util.Optional;
import org.ptit.meeting.modules.ai.entity.RecordingSubtitleReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordingSubtitleReviewRepository
    extends JpaRepository<RecordingSubtitleReview, String> {
  Optional<RecordingSubtitleReview> findByRecordingId(String recordingId);
}
