package org.ptit.meeting.modules.timetable.repository;

import java.util.Optional;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng subjects (Môn học).
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

  Optional<Subject> findByCode(String code);

  boolean existsByCode(String code);

}
