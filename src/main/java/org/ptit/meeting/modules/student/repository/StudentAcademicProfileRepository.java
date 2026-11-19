package org.ptit.meeting.modules.student.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.student.entity.StudentAcademicProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAcademicProfileRepository extends JpaRepository<StudentAcademicProfile, Long> {
  @EntityGraph(attributePaths = {"administrativeClass", "administrativeClass.campus",
      "administrativeClass.cohort", "administrativeClass.major"})
  Optional<StudentAcademicProfile> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"administrativeClass", "administrativeClass.campus",
      "administrativeClass.cohort", "administrativeClass.major"})
  List<StudentAcademicProfile> findAllByUserIdIn(Collection<Long> userIds);

  @Query("""
      SELECT p.administrativeClass.id, COUNT(p.id)
      FROM StudentAcademicProfile p
      GROUP BY p.administrativeClass.id
      """)
  List<Object[]> countStudentsByAdministrativeClass();

  @Query("SELECT COUNT(p) FROM StudentAcademicProfile p")
  long countAssignedStudents();
}
