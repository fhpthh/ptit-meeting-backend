package org.ptit.meeting.modules.timetable.repository;

import java.util.List;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác với bảng class_schedules (Lịch học định kỳ theo tuần). Lưu trữ và truy vấn
 * cấu hình lịch học tuần (Thứ, Tiết, Tuần học). Phục vụ việc phát hiện trùng ca dạy của Giảng
 * viên.
 */
@Repository
public interface ClassSchedulerRepository extends JpaRepository<ClassSchedule, Long> {

  List<ClassSchedule> findAllByClassSectionId(Long classSectionId);

  List<ClassSchedule> findAllByClassSectionIdAndIsOnlineTrue(Long classSectionId);

  @Query("SELECT cs FROM ClassSchedule cs "
      + "JOIN ClassSection sec ON cs.classSectionId = sec.id "
      + "WHERE sec.semesterId = :semesterId AND sec.lecturerId = :lecturerId")
  List<ClassSchedule> findAllByLecturerAndSemester(
      @Param("semesterId") Long semesterId,
      @Param("lecturerId") Long lecturerId
  );
}
