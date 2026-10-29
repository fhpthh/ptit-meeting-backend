package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
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
}