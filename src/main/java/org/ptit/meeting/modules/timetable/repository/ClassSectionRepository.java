package org.ptit.meeting.modules.timetable.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterClassSectionCountRow;
import org.ptit.meeting.modules.timetable.dto.internal.ClassSectionListRow;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT section FROM ClassSection section WHERE section.id = :id")
  Optional<ClassSection> findByIdForUpdate(@Param("id") Long id);

  Optional<ClassSection> findByClassCodeAndSemesterId(String classCode, Long semesterId);

  List<ClassSection> findAllBySemesterId(Long semesterId);

  List<ClassSection> findAllBySemesterIdAndLecturerId(Long semesterId, Long lecturerId);

  List<ClassSection> findAllBySemesterIdAndSubjectIdAndCohort(
      Long semesterId,
      Long subjectId,
      String cohort
  );

  Optional<ClassSection> findByIdAndSemesterIdAndSubjectIdAndCohort(
      Long id,
      Long semesterId,
      Long subjectId,
      String cohort
  );

  @Query(
      value = """
          SELECT new org.ptit.meeting.modules.timetable.dto.internal.ClassSectionListRow(
              section.id, section.semesterId, section.subjectId, section.lecturerId,
              section.lecturerName, section.cohort, section.classCode, section.groupNumber,
              section.practiceGroup, section.managementClass, section.status,
              section.expectedStudentCount, COUNT(DISTINCT student.id)
          )
          FROM ClassSection section
          LEFT JOIN ClassEnrollment enrollment
            ON enrollment.classSectionId = section.id AND enrollment.status = :enrollmentStatus
          LEFT JOIN User student ON student.id = enrollment.userId
            AND student.accountType = org.ptit.meeting.modules.auth.constant.enums.AccountType.STUDENT
          LEFT JOIN ClassSchedule schedule ON schedule.classSectionId = section.id
          WHERE section.semesterId = :semesterId
            AND section.subjectId = :subjectId
            AND section.cohort = :cohort
            AND (:keyword IS NULL
              OR LOWER(section.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(section.lecturerName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          GROUP BY section.id, section.semesterId, section.subjectId, section.lecturerId,
              section.lecturerName, section.cohort, section.classCode, section.groupNumber,
              section.practiceGroup, section.managementClass, section.status,
              section.expectedStudentCount
          ORDER BY
            CASE WHEN :sortBy = 'classCode' AND :direction = 'ASC' THEN section.classCode END ASC,
            CASE WHEN :sortBy = 'classCode' AND :direction = 'DESC' THEN section.classCode END DESC,
            CASE WHEN :sortBy = 'groupNumber' AND :direction = 'ASC' THEN section.groupNumber END ASC,
            CASE WHEN :sortBy = 'groupNumber' AND :direction = 'DESC' THEN section.groupNumber END DESC,
            CASE WHEN :sortBy = 'lecturerName' AND :direction = 'ASC' THEN section.lecturerName END ASC,
            CASE WHEN :sortBy = 'lecturerName' AND :direction = 'DESC' THEN section.lecturerName END DESC,
            CASE WHEN :sortBy = 'enrolledStudentCount' AND :direction = 'ASC' THEN COUNT(DISTINCT student.id) END ASC,
            CASE WHEN :sortBy = 'enrolledStudentCount' AND :direction = 'DESC' THEN COUNT(DISTINCT student.id) END DESC,
            CASE WHEN :sortBy = 'schedule' AND :direction = 'ASC' THEN MIN(schedule.dayOfWeek) END ASC,
            CASE WHEN :sortBy = 'schedule' AND :direction = 'ASC' THEN MIN(schedule.startTime) END ASC,
            CASE WHEN :sortBy = 'schedule' AND :direction = 'DESC' THEN MIN(schedule.dayOfWeek) END DESC,
            CASE WHEN :sortBy = 'schedule' AND :direction = 'DESC' THEN MIN(schedule.startTime) END DESC,
            CASE WHEN :sortBy = 'room' AND :direction = 'ASC' THEN MIN(schedule.roomText) END ASC,
            CASE WHEN :sortBy = 'room' AND :direction = 'DESC' THEN MIN(schedule.roomText) END DESC,
            section.classCode ASC
          """,
      countQuery = """
          SELECT COUNT(section.id)
          FROM ClassSection section
          WHERE section.semesterId = :semesterId
            AND section.subjectId = :subjectId
            AND section.cohort = :cohort
            AND (:keyword IS NULL
              OR LOWER(section.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(section.lecturerName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          """
  )
  Page<ClassSectionListRow> searchAdminSections(
      @Param("semesterId") Long semesterId,
      @Param("subjectId") Long subjectId,
      @Param("cohort") String cohort,
      @Param("keyword") String keyword,
      @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
      @Param("sortBy") String sortBy,
      @Param("direction") String direction,
      Pageable pageable
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
