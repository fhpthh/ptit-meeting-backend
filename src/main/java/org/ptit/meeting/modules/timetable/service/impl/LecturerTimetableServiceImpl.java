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
import org.ptit.meeting.modules.timetable.constant.enums.RoomStatus;
import org.ptit.meeting.modules.timetable.dto.internal.LecturerOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.internal.LecturerScheduleTimeRow;
import org.ptit.meeting.modules.timetable.dto.response.LecturerSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.LecturerWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.mapper.LecturerTimetableMapper;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.LecturerTimetableService;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LecturerTimetableServiceImpl
    implements LecturerTimetableService {

  private final SemesterRepository semesterRepository;
  private final ClassSchedulerRepository classSchedulerRepository;
  private final MeetingSessionRepository meetingSessionRepository;
  private final LecturerTimetableMapper lecturerTimetableMapper;
  private final ScheduleDateCalculator dateCalculator;

  @Override
  @Transactional(readOnly = true)
  public List<LecturerSemesterResponse> getAvailableSemesters(
      Long lecturerId
  ) {
    log.info(
        "(getAvailableSemesters) lecturerId: {}",
        lecturerId
    );

    return semesterRepository
        .findAllAvailableForLecturer(
            lecturerId,
            ClassSectionStatus.ACTIVE
        )
        .stream()
        .map(lecturerTimetableMapper::toSemesterResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LecturerWeeklyTimetableResponse getWeeklyTimetable(
      Long lecturerId,
      Long semesterId,
      int weekNumber
  ) {
    log.info(
        "(getWeeklyTimetable) lecturerId: {}, semesterId: {}, weekNumber: {}",
        lecturerId,
        semesterId,
        weekNumber
    );

    Semester semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SEMESTER_NOT_FOUND,
            semesterId
        ));

    validateLecturerSemesterAccess(lecturerId, semesterId);

    LocalDate weekStart = calculateWeekStart(
        semester,
        weekNumber
    );

    LocalDate weekEnd = weekStart.plusDays(6);
    if (weekEnd.isAfter(semester.getEndDate())) {
      weekEnd = semester.getEndDate();
    }

    List<LecturerOnlineScheduleRow> scheduleRows =
        meetingSessionRepository.findLecturerWeeklyOnlineSchedule(
            lecturerId,
            semesterId,
            weekStart,
            weekEnd,
            ClassSectionStatus.ACTIVE,
            RoomStatus.ACTIVE
        );

    List<LecturerWeeklyTimetableResponse.TimetableItem> items =
        scheduleRows.stream()
            .map(lecturerTimetableMapper::toTimetableItem)
            .toList();

    List<LecturerScheduleTimeRow> semesterTimes =
        meetingSessionRepository.findLecturerSemesterOnlineSessionTimes(
            lecturerId,
            semesterId,
            ClassSectionStatus.ACTIVE
        );

    return new LecturerWeeklyTimetableResponse(
        lecturerTimetableMapper.toSemesterResponse(semester),
        new LecturerWeeklyTimetableResponse.WeekInfo(
            weekNumber,
            weekStart,
            weekEnd
        ),
        buildTimeline(semesterTimes),
        items
    );
  }

  private void validateLecturerSemesterAccess(
      Long lecturerId,
      Long semesterId
  ) {
    boolean hasAccess =
        classSchedulerRepository.existsOnlineScheduleForLecturer(
            lecturerId,
            semesterId,
            ClassSectionStatus.ACTIVE
        );

    if (!hasAccess) {
      throw BusinessException.unauthorized(
          TimetableErrorConstants.LECTURER_SEMESTER_ACCESS_DENIED,
          semesterId
      );
    }
  }

  private LocalDate calculateWeekStart(
      Semester semester,
      int weekNumber
  ) {
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

    return weekStart;
  }

  private LecturerWeeklyTimetableResponse.TimelineInfo buildTimeline(
      List<LecturerScheduleTimeRow> sessionTimes
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

    return new LecturerWeeklyTimetableResponse.TimelineInfo(
        startTime,
        endTime
    );
  }
}