package org.ptit.meeting.modules.timetable.repository;

import java.time.LocalDate;
import java.util.List;
import org.ptit.meeting.modules.timetable.entity.MeetingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingSessionRepository extends JpaRepository<MeetingSession, Long> {

  boolean existsByScheduleIdAndStudyDate(Long scheduleId, LocalDate date);

  List<MeetingSession> findAllByRoomIdAndStudyDate(Long roomId, LocalDate date);

  /**
   * Lấy danh sách các buổi học Online trong ngày của một Sinh viên (cho giao diện TKB Sinh viên).
   */
  @Query("SELECT ms FROM MeetingSession ms "
      + "JOIN ClassSchedule csc ON ms.scheduleId = csc.id "
      + "JOIN ClassSection cs ON csc.classSectionId = cs.id "
      + "JOIN ClassEnrollment ce ON ce.classSectionId = cs.id "
      + "WHERE ce.studentId = :studentId "
      + "  AND ms.studyDate = :studyDate "
      + "  AND csc.isOnline = true "
      + "ORDER BY ms.scheduledStart ASC")
  List<MeetingSession> findStudentDailyOnlineSessions(
      @Param("studentId") Long studentId,
      @Param("studyDate") LocalDate studyDate
  );

  /**
   * Lấy danh sách các buổi học Online trong ngày của Giảng viên (cho giao diện Lịch dạy Giảng
   * viên).
   */
  @Query("SELECT ms FROM MeetingSession ms "
      + "JOIN ClassSchedule csc ON ms.scheduleId = csc.id "
      + "JOIN ClassSection cs ON csc.classSectionId = cs.id "
      + "WHERE cs.lecturerId = :lecturerId "
      + "  AND ms.studyDate = :studyDate "
      + "  AND csc.isOnline = true "
      + "ORDER BY ms.scheduledStart ASC")
  List<MeetingSession> findLecturerDailyOnlineSessions(
      @Param("lecturerId") Long lecturerId,
      @Param("studyDate") LocalDate studyDate
  );
}
