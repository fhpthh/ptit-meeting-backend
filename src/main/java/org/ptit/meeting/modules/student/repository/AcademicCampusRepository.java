package org.ptit.meeting.modules.student.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.student.entity.AcademicCampus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicCampusRepository extends JpaRepository<AcademicCampus, Long> {
  List<AcademicCampus> findAllByActiveTrueOrderByNameAsc();
  Optional<AcademicCampus> findByCodeIgnoreCase(String code);
}
