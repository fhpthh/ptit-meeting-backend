package org.ptit.meeting.modules.ai.repository;

import java.time.Instant;
import java.util.List;
import org.ptit.meeting.modules.ai.dto.internal.RecordingDispatchSnapshot;
import org.ptit.meeting.modules.ai.entity.RecordingJobOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordingJobOutboxRepository extends JpaRepository<RecordingJobOutbox, String> {
  @Query(
      "select new"
          + " org.ptit.meeting.modules.ai.dto.internal.RecordingDispatchSnapshot(o.id,j.id,j.recordingId,j.runId,j.recordingEpoch,o.dispatchVersion)"
          + " from RecordingJobOutbox o, RecordingTranslationJob j where o.jobId=j.id and"
          + " o.availableAt<=:now and (o.publishedAt is null or o.publishedAt<=:cutoff) and"
          + " j.status in"
          + " (org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus.QUEUED,org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStatus.RETRY_WAIT)"
          + " and o.dispatchVersion=j.attempt+1 order by o.availableAt,o.id")
  List<RecordingDispatchSnapshot> due(
      @Param("now") Instant now, @Param("cutoff") Instant cutoff, Pageable page);

  @Modifying
  @Query("update RecordingJobOutbox o set o.publishedAt=:time where o.id=:id")
  int published(@Param("id") String id, @Param("time") Instant time);
}
