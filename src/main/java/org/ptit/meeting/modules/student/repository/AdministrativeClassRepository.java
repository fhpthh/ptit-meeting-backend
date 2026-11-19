package org.ptit.meeting.modules.student.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.student.entity.AdministrativeClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeClassRepository extends JpaRepository<AdministrativeClass, Long> {
  @EntityGraph(attributePaths = {"campus", "cohort", "major"})
  List<AdministrativeClass> findAllByActiveTrueOrderByCodeAsc();

  @EntityGraph(attributePaths = {"campus", "cohort", "major"})
  Optional<AdministrativeClass> findWithRelationsById(Long id);

  Optional<AdministrativeClass> findByCodeIgnoreCase(String code);
}
