package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng class_sections (Lớp học phần).
 */
@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

  Optional<ClassSection> findByClassCodeAndSemesterId(String classCode, Long semesterId);

  List<ClassSection> findAllBySemesterId(Long semesterId);

  List<ClassSection> findAllBySemesterIdAndLecturerId(Long semesterId, Long lecturerId);

  boolean existsByClassCodeAndSemesterId(String classCode, Long semesterId);
}
