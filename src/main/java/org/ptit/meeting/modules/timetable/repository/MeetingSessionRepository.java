package org.ptit.meeting.modules.timetable.repository;

import java.time.LocalDate;
import java.util.List;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.constant.enums.RoomStatus;
import org.ptit.meeting.modules.timetable.dto.internal.StudentOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.internal.StudentScheduleTimeRow;
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

  @Query("""
    SELECT new org.ptit.meeting.modules.timetable.dto.internal.StudentOnlineScheduleRow(
        session.id,
        session.scheduleId,
        session.roomId,
        session.studyDate,
        session.scheduledStart,
        session.scheduledEnd,
        session.status,

        schedule.startPeriod,
        schedule.numPeriods,

        section.id,
        section.classCode,
        section.groupNumber,
        section.practiceGroup,

        section.lecturerId,
        section.lecturerName,

        subject.code,
        subject.name,

        room.name
    )
    FROM MeetingSession session
    JOIN ClassSchedule schedule
      ON schedule.id = session.scheduleId
    JOIN ClassSection section
      ON section.id = schedule.classSectionId
    JOIN ClassEnrollment enrollment
      ON enrollment.classSectionId = section.id
    JOIN Subject subject
      ON subject.id = section.subjectId
    JOIN Room room
      ON room.id = session.roomId
    WHERE enrollment.studentId = :studentId
      AND enrollment.status = :enrollmentStatus
      AND section.semesterId = :semesterId
      AND section.status = :sectionStatus
      AND room.status = :roomStatus
      AND schedule.isOnline = true
      AND session.studyDate BETWEEN :weekStart AND :weekEnd
    ORDER BY session.studyDate ASC, session.scheduledStart ASC
    """)
  List<StudentOnlineScheduleRow> findStudentWeeklyOnlineSchedule(
    @Param("studentId") Long studentId,
    @Param("semesterId") Long semesterId,
    @Param("weekStart") LocalDate weekStart,
    @Param("weekEnd") LocalDate weekEnd,
    @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
    @Param("sectionStatus") ClassSectionStatus sectionStatus,
    @Param("roomStatus") RoomStatus roomStatus
  );

  @Query("""
    SELECT new org.ptit.meeting.modules.timetable.dto.internal.StudentScheduleTimeRow(
        session.scheduledStart,
        session.scheduledEnd
    )
    FROM MeetingSession session
    JOIN ClassSchedule schedule
      ON schedule.id = session.scheduleId
    JOIN ClassSection section
      ON section.id = schedule.classSectionId
    JOIN ClassEnrollment enrollment
      ON enrollment.classSectionId = section.id
    WHERE enrollment.studentId = :studentId
      AND enrollment.status = :enrollmentStatus
      AND section.semesterId = :semesterId
      AND section.status = :sectionStatus
      AND schedule.isOnline = true
    """)
  List<StudentScheduleTimeRow> findStudentSemesterOnlineSessionTimes(
      @Param("studentId") Long studentId,
      @Param("semesterId") Long semesterId,
      @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
      @Param("sectionStatus") ClassSectionStatus sectionStatus
  );
}
