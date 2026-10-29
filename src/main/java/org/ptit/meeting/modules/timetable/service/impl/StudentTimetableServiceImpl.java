package org.ptit.meeting.modules.timetable.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.constant.enums.RoomStatus;
import org.ptit.meeting.modules.timetable.dto.internal.StudentOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.internal.StudentScheduleTimeRow;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.mapper.StudentTimetableMapper;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.StudentTimetableService;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentTimetableServiceImpl
    implements StudentTimetableService {

  private final SemesterRepository semesterRepository;
  private final MeetingSessionRepository meetingSessionRepository;
  private final StudentTimetableMapper studentTimetableMapper;
  private final ScheduleDateCalculator dateCalculator;

  @Override
  @Transactional(readOnly = true)
  public List<StudentSemesterResponse> getAvailableSemesters(
      Long studentId
  ) {
    log.info(
        "(getAvailableSemesters) studentId: {}",
        studentId
    );

    return semesterRepository
        .findAllAvailableForStudent(
            studentId,
            EnrollmentStatus.ENROLLED
        )
        .stream()
        .map(studentTimetableMapper::toSemesterResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public StudentWeeklyTimetableResponse getWeeklyTimetable(
      Long studentId,
      Long semesterId,
      int weekNumber
  ) {
    log.info(
        "(getWeeklyTimetable) studentId: {}, semesterId: {}, weekNumber: {}",
        studentId,
        semesterId,
        weekNumber
    );

    Semester semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SEMESTER_NOT_FOUND,
            semesterId
        ));

    LocalDate weekStart;
    try {
      weekStart = dateCalculator.calculateWeekStart(
          semester.getStartDate(),
          weekNumber
      );
    } catch (IllegalArgumentException exception) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.WEEK_INVALID,
          weekNumber
      );
    }

    if (weekStart.isAfter(semester.getEndDate())) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.WEEK_INVALID,
          weekNumber
      );
    }

    LocalDate weekEnd = weekStart.plusDays(6);

    if (weekEnd.isAfter(semester.getEndDate())) {
      weekEnd = semester.getEndDate();
    }

    List<StudentOnlineScheduleRow> scheduleRows =
        meetingSessionRepository.findStudentWeeklyOnlineSchedule(
            studentId,
            semesterId,
            weekStart,
            weekEnd,
            EnrollmentStatus.ENROLLED,
            ClassSectionStatus.ACTIVE,
            RoomStatus.ACTIVE
        );

    List<StudentWeeklyTimetableResponse.TimetableItem> items =
        scheduleRows.stream()
            .map(studentTimetableMapper::toTimetableItem)
            .toList();

    List<StudentScheduleTimeRow> semesterSessionTimes =
        meetingSessionRepository.findStudentSemesterOnlineSessionTimes(
            studentId,
            semesterId,
            EnrollmentStatus.ENROLLED,
            ClassSectionStatus.ACTIVE
        );

    StudentWeeklyTimetableResponse.TimelineInfo timeline = buildTimeline(
        semesterSessionTimes
    );

    return new StudentWeeklyTimetableResponse(
        studentTimetableMapper.toSemesterResponse(semester),
        new StudentWeeklyTimetableResponse.WeekInfo(
            weekNumber,
            weekStart,
            weekEnd
        ),
        timeline,
        items
    );
  }

  private StudentWeeklyTimetableResponse.TimelineInfo buildTimeline(
      List<StudentScheduleTimeRow> sessionTimes
  ) {
    if (sessionTimes.isEmpty()) {
      return null;
    }

    LocalTime startTime = sessionTimes.stream()
        .map(row -> row.scheduledStart().toLocalTime())
        .min(Comparator.naturalOrder())
        .orElseThrow();
    LocalTime endTime = sessionTimes.stream()
        .map(row -> row.scheduledEnd().toLocalTime())
        .max(Comparator.naturalOrder())
        .orElseThrow();

    return new StudentWeeklyTimetableResponse.TimelineInfo(
        startTime,
        endTime
    );
  }

}
