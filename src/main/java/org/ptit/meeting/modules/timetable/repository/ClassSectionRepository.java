package org.ptit.meeting.modules.timetable.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterClassSectionCountRow;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng class_sections (Lớp học phần).
 */
@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

  Optional<ClassSection> findByClassCodeAndSemesterId(String classCode, Long semesterId);

  List<ClassSection> findAllBySemesterId(Long semesterId);

  List<ClassSection> findAllBySemesterIdAndLecturerId(Long semesterId, Long lecturerId);

  List<ClassSection> findAllBySemesterIdAndSubjectIdAndCohort(
      Long semesterId,
      Long subjectId,
      String cohort
  );

  boolean existsByClassCodeAndSemesterId(String classCode, Long semesterId);

  boolean existsBySemesterId(Long semesterId);

  @Query("""
      SELECT DISTINCT section.cohort
      FROM ClassSection section
      WHERE section.semesterId = :semesterId
        AND section.cohort IS NOT NULL
        AND section.cohort <> ''
      ORDER BY section.cohort
      """)
  List<String> findCohortsBySemesterId(@Param("semesterId") Long semesterId);

  @Query("""
      SELECT DISTINCT section.managementClass
      FROM ClassSection section
      WHERE section.semesterId = :semesterId
        AND section.managementClass IS NOT NULL
        AND section.managementClass <> ''
      ORDER BY section.managementClass
      """)
  List<String> findManagementClassesBySemesterId(@Param("semesterId") Long semesterId);

  @Query("""
      SELECT new org.ptit.meeting.modules.timetable.dto.internal.SemesterClassSectionCountRow(
          section.semesterId,
          COUNT(section.id)
      )
      FROM ClassSection section
      WHERE section.semesterId IN :semesterIds
      GROUP BY section.semesterId
      """)
  List<SemesterClassSectionCountRow> countBySemesterIds(
      @Param("semesterIds") Set<Long> semesterIds
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM ClassSection c WHERE c.id = :id")
  Optional<ClassSection> findLockedById(@Param("id") Long id);

}
