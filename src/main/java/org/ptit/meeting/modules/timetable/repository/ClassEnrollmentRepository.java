package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.dto.internal.ClassSectionStudentRow;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassEnrollmentRepository
    extends JpaRepository<ClassEnrollment, Long> {

  List<ClassEnrollment> findAllByStudentIdAndStatus(
      Long studentId,
      EnrollmentStatus status
  );

  boolean existsByClassSectionIdAndStudentIdAndStatus(
      Long classSectionId,
      Long studentId,
      EnrollmentStatus status
  );

  boolean existsByClassSectionIdAndStudentId(Long classSectionId, Long studentId);

  long countByClassSectionIdAndStatus(Long classSectionId, EnrollmentStatus status);

  @Query(
      value = """
          SELECT new org.ptit.meeting.modules.timetable.dto.internal.ClassSectionStudentRow(
              enrollment.id, student.id, student.code, student.fullName, student.email,
              student.department, enrollment.status, enrollment.enrolledAt
          )
          FROM ClassEnrollment enrollment
          JOIN User student ON student.id = enrollment.studentId
          WHERE enrollment.classSectionId = :classSectionId
            AND enrollment.status = :status
            AND (:keyword IS NULL
              OR LOWER(student.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.department) LIKE LOWER(CONCAT('%', :keyword, '%')))
          ORDER BY
            CASE WHEN :sortBy = 'studentCode' AND :direction = 'ASC' THEN student.code END ASC,
            CASE WHEN :sortBy = 'studentCode' AND :direction = 'DESC' THEN student.code END DESC,
            CASE WHEN :sortBy = 'fullName' AND :direction = 'ASC' THEN student.fullName END ASC,
            CASE WHEN :sortBy = 'fullName' AND :direction = 'DESC' THEN student.fullName END DESC,
            CASE WHEN :sortBy = 'managementClass' AND :direction = 'ASC' THEN student.department END ASC,
            CASE WHEN :sortBy = 'managementClass' AND :direction = 'DESC' THEN student.department END DESC,
            CASE WHEN :sortBy = 'enrolledAt' AND :direction = 'ASC' THEN enrollment.enrolledAt END ASC,
            CASE WHEN :sortBy = 'enrolledAt' AND :direction = 'DESC' THEN enrollment.enrolledAt END DESC,
            student.code ASC
          """,
      countQuery = """
          SELECT COUNT(enrollment.id)
          FROM ClassEnrollment enrollment
          JOIN User student ON student.id = enrollment.studentId
          WHERE enrollment.classSectionId = :classSectionId
            AND enrollment.status = :status
            AND (:keyword IS NULL
              OR LOWER(student.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(student.department) LIKE LOWER(CONCAT('%', :keyword, '%')))
          """
  )
  Page<ClassSectionStudentRow> searchStudents(
      @Param("classSectionId") Long classSectionId,
      @Param("status") EnrollmentStatus status,
      @Param("keyword") String keyword,
      @Param("sortBy") String sortBy,
      @Param("direction") String direction,
      Pageable pageable
  );

}
