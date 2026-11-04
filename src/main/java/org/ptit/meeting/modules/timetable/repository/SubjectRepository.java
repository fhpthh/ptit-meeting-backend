package org.ptit.meeting.modules.timetable.repository;

import java.util.Optional;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterSubjectRow;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng subjects (Môn học).
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

  Optional<Subject> findByCode(String code);

  boolean existsByCode(String code);

  @Query(
      value = """
          SELECT new org.ptit.meeting.modules.timetable.dto.internal.SemesterSubjectRow(
              subject.id, subject.code, subject.name, subject.credit,
              subject.department, COUNT(section.id)
          )
          FROM Subject subject
          JOIN ClassSection section ON section.subjectId = subject.id
          WHERE section.semesterId = :semesterId
            AND section.cohort = :cohort
            AND (:department IS NULL OR subject.department = :department)
            AND (:managementClass IS NULL OR section.managementClass LIKE CONCAT('%', :managementClass, '%'))
            AND (:keyword IS NULL OR LOWER(subject.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(subject.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          GROUP BY subject.id, subject.code, subject.name, subject.credit, subject.department
          """,
      countQuery = """
          SELECT COUNT(DISTINCT subject.id)
          FROM Subject subject
          JOIN ClassSection section ON section.subjectId = subject.id
          WHERE section.semesterId = :semesterId
            AND section.cohort = :cohort
            AND (:department IS NULL OR subject.department = :department)
            AND (:managementClass IS NULL OR section.managementClass LIKE CONCAT('%', :managementClass, '%'))
            AND (:keyword IS NULL OR LOWER(subject.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(subject.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          """
  )
  Page<SemesterSubjectRow> searchBySemester(
      @Param("semesterId") Long semesterId,
      @Param("cohort") String cohort,
      @Param("department") String department,
      @Param("managementClass") String managementClass,
      @Param("keyword") String keyword,
      Pageable pageable
  );

  @Query("""
      SELECT DISTINCT subject.department
      FROM Subject subject
      JOIN ClassSection section ON section.subjectId = subject.id
      WHERE section.semesterId = :semesterId
        AND subject.department IS NOT NULL
        AND subject.department <> ''
      ORDER BY subject.department
      """)
  java.util.List<String> findDepartmentsBySemesterId(@Param("semesterId") Long semesterId);

}
