package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

  List<ImportHistory> findAllBySemesterIdOrderByCreatedAtDesc(Long semesterId);

  List<ImportHistory> findAllByUploadedByOrderByCreatedAtDesc(Long uploadedBy);
}
