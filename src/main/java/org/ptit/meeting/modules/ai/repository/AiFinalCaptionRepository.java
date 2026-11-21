package org.ptit.meeting.modules.ai.repository;

import org.ptit.meeting.modules.ai.entity.AiFinalCaption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFinalCaptionRepository extends JpaRepository<AiFinalCaption, String> {
  boolean existsByRuntimeSessionIdAndStreamIdAndGenerationAndSegmentIdAndLanguageAndRevision(
      String runtimeSessionId, String streamId, long generation, String segmentId,
      String language, long revision);
}
