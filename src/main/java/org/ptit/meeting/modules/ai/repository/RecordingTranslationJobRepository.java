package org.ptit.meeting.modules.ai.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.ai.entity.RecordingTranslationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordingTranslationJobRepository
    extends JpaRepository<RecordingTranslationJob, String> {
  Optional<RecordingTranslationJob> findByRecordingIdAndRequestedByUserIdAndIdempotencyHash(
      String recordingId, Long userId, String idempotencyHash);

  Optional<RecordingTranslationJob> findByActiveSlot(String recordingId);

  List<RecordingTranslationJob> findTop50ByRecordingIdOrderByCreatedAtDesc(String recordingId);

  Optional<RecordingTranslationJob> findFirstByRecordingIdOrderByCreatedAtDesc(String recordingId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select job from RecordingTranslationJob job where job.id = :id")
  Optional<RecordingTranslationJob> findByIdForUpdate(@Param("id") String id);
}
