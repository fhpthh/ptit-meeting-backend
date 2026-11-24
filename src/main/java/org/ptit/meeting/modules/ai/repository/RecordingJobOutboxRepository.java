package org.ptit.meeting.modules.ai.repository;

import org.ptit.meeting.modules.ai.entity.RecordingJobOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordingJobOutboxRepository extends JpaRepository<RecordingJobOutbox, String> {}
