package org.ptit.meeting.modules.ai.repository;

import java.util.Optional;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordingTranslationDraftRepository
    extends JpaRepository<RecordingTranslationDraft, String> {
  Optional<RecordingTranslationDraft> findByJobId(String jobId);
}
