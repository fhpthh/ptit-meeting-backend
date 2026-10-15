package org.ptit.meeting.modules.timetable.repository;

import java.util.Optional;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng semesters (Quản lý Học kỳ & Năm học).
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

  Optional<Semester> findByCode(String code);

  boolean existsByCode(String code);

  Optional<Semester> findByIsActiveTrue();

}
