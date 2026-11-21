package org.ptit.meeting.modules.student.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.student.entity.AdmissionCohort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionCohortRepository extends JpaRepository<AdmissionCohort, Long> {
  List<AdmissionCohort> findAllByActiveTrueOrderByCodeDesc();
  Optional<AdmissionCohort> findByCodeIgnoreCase(String code);
}
