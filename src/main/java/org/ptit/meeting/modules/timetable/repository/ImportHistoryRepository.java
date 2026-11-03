package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.dto.internal.ImportHistoryListRow;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

  boolean existsBySemesterId(Long semesterId);

  List<ImportHistory> findAllBySemesterIdOrderByCreatedAtDesc(Long semesterId);

  List<ImportHistory> findAllByUploadedByOrderByCreatedAtDesc(Long uploadedBy);

  Optional<ImportHistory> findFirstBySemesterIdAndStatusOrderByCreatedAtDesc(
      Long semesterId,
      ImportStatus status
  );

  @Query("""
      SELECT new org.ptit.meeting.modules.timetable.dto.internal.ImportHistoryListRow(
          h.id,
          h.semesterId,
          h.uploadedBy,
          h.fileName,
          h.importType,
          h.totalRows,
          h.successRows,
          h.failedRows,
          h.status,
          h.createdAt,
          h.updatedAt
      )
      FROM ImportHistory h
      WHERE (:semesterId IS NULL OR h.semesterId = :semesterId)
        AND (:status IS NULL OR h.status = :status)
      """)
  Page<ImportHistoryListRow> search(
      @Param("semesterId") Long semesterId,
      @Param("status") ImportStatus status,
      Pageable pageable
  );
}
