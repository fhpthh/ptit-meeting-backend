package org.ptit.meeting.modules.timetable.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng semesters (Quản lý Học kỳ & Năm học).
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

  Optional<Semester> findByCode(String code);

  boolean existsByCode(String code);

  Optional<Semester> findByIsActiveTrue();

  @Query("""
      SELECT DISTINCT semester
      FROM Semester semester
      JOIN ClassSection section
        ON section.semesterId = semester.id
      JOIN ClassEnrollment enrollment
        ON enrollment.classSectionId = section.id
      WHERE enrollment.userId = :studentId
        AND enrollment.status = :enrollmentStatus
      ORDER BY semester.startDate DESC
      """)
  List<Semester> findAllAvailableForStudent(
      @Param("studentId") Long studentId,
      @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus
  );

  @Query("""
      SELECT DISTINCT semester
      FROM Semester semester
      JOIN ClassSection section
        ON section.semesterId = semester.id
      JOIN ClassSchedule schedule
        ON schedule.classSectionId = section.id
      WHERE section.lecturerId = :lecturerId
        AND section.status = :sectionStatus
        AND schedule.isOnline = true
      ORDER BY semester.startDate DESC
      """)
  List<Semester> findAllAvailableForLecturer(
      @Param("lecturerId") Long lecturerId,
      @Param("sectionStatus") ClassSectionStatus sectionStatus
  );

  boolean existsByCodeAndIdNot(String code, Long id);

  @Query("""
      SELECT semester
      FROM Semester semester
      WHERE (
          :keyword IS NULL
          OR LOWER(semester.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(semester.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (
          :academicYear IS NULL
          OR semester.academicYear = :academicYear
      )
      """)
  Page<Semester> search(
      @Param("keyword") String keyword,
      @Param("academicYear") String academicYear,
      Pageable pageable
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT semester FROM Semester semester")
  List<Semester> findAllForUpdate();
}
