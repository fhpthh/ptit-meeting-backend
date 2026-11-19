package org.ptit.meeting.modules.student.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.student.entity.AcademicMajor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicMajorRepository extends JpaRepository<AcademicMajor, Long> {
  List<AcademicMajor> findAllByActiveTrueOrderByNameAsc();
  Optional<AcademicMajor> findByCodeIgnoreCase(String code);
}
