package org.ptit.meeting.modules.timetable.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.ptit.meeting.modules.timetable.service.TimetableService;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

  private final SemesterRepository semesterRepository;
  private final SubjectRepository subjectRepository;
  private final ClassSectionRepository classSectionRepository;
  private final ClassSchedulerRepository classScheduleRepository;
  private final RoomRepository roomRepository;
  private final MeetingSessionRepository sessionRepository;
  private final UserService userService;
  private final MeetingRoomService meetingRoomService;
  private final ScheduleDateCalculator dateCalculator;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public TimetableProcessResult processMasterTimetable(Long semesterId,
      List<MasterTimetableRowDto> rows,
      List<ImportRowError> errors) {
    Semester semester = semesterRepository.findById(semesterId)
        .orElseThrow(
            () -> BusinessException.notExisted(TimetableErrorConstants.SEMESTER_NOT_FOUND));

    int successCount = 0;
    Set<String> processedSubjects = new HashSet<>();
    Set<String> processedClassSections = new HashSet<>();
    int schedulesSaved = 0;
    AtomicInteger onlineRoomsAllocated = new AtomicInteger(0);
    AtomicInteger meetingSessionsGenerated = new AtomicInteger(0);

    for (MasterTimetableRowDto row : rows) {
      try {
        processSingleRow(semester, row, processedSubjects, processedClassSections,
            onlineRoomsAllocated, meetingSessionsGenerated);
        successCount++;
        schedulesSaved++;
      } catch (Exception e) {
        log.warn("(processMasterTimetable) Error processing row {}: {}", row.getRowNumber(),
            e.getMessage());
        errors.add(new ImportRowError(row.getRowNumber(), e.getMessage()));
      }
    }

    return TimetableProcessResult.builder()
        .successCount(successCount)
        .subjectsProcessed(processedSubjects.size())
        .classSectionsProcessed(processedClassSections.size())
        .classSchedulesSaved(schedulesSaved)
        .onlineRoomsAllocated(onlineRoomsAllocated.get())
        .meetingSessionsGenerated(meetingSessionsGenerated.get())
        .build();
  }

  private void processSingleRow(
      Semester semester,
      MasterTimetableRowDto dto,
      Set<String> processedSubjects,
      Set<String> processedClassSections,
      AtomicInteger onlineRoomsAllocated,
      AtomicInteger meetingSessionsGenerated
  ) {
    Subject subject = upsertSubject(dto);
    processedSubjects.add(subject.getCode());

    ClassSection section = upsertClassSection(semester.getId(), subject.getId(), dto);
    processedClassSections.add(section.getClassCode());

    ClassSchedule schedule = upsertClassSchedule(section.getId(), dto);

    if (dto.isOnline()) {
      Room room = allocateOnlineRoom(section, onlineRoomsAllocated);
      int sessionsCount = generateOnlineSessions(semester.getStartDate(), schedule, room, dto);
      meetingSessionsGenerated.addAndGet(sessionsCount);
    }
  }

  private Subject upsertSubject(MasterTimetableRowDto dto) {
    return subjectRepository.findByCode(dto.getSubjectCode())
        .orElseGet(() -> subjectRepository.save(
            Subject.builder()
                .code(dto.getSubjectCode())
                .name(dto.getSubjectName())
                .credit(dto.getCredit() != null ? dto.getCredit() : 3)
                .build()
        ));
  }

  private ClassSection upsertClassSection(Long semesterId, Long subjectId,
      MasterTimetableRowDto dto) {
    User lecturer = userService.findLecturer(dto.getLecturerName()).orElse(null);
    Long lecturerId = lecturer == null ? null : lecturer.getId();
    return classSectionRepository.findByClassCodeAndSemesterId(dto.getClassCode(), semesterId)
        .map(existing -> {
          if (lecturerId != null && existing.getLecturerId() == null) {
            existing.setLecturerId(lecturerId);
            existing.setLecturerName(lecturer.getFullName());
            meetingRoomService.refreshClassAccess(existing.getId());
          }
          if (existing.getLecturerId() == null && dto.getLecturerName() != null) {
            existing.setLecturerName(dto.getLecturerName());
          }
          return classSectionRepository.save(existing);
        })
        .orElseGet(() -> classSectionRepository.save(
            ClassSection.builder()
                .semesterId(semesterId)
                .subjectId(subjectId)
                .lecturerId(lecturerId)
                .lecturerName(lecturer == null ? dto.getLecturerName() : lecturer.getFullName())
                .cohort(dto.getCohort())
                .classCode(dto.getClassCode())
                .groupNumber(dto.getGroupNumber())
                .practiceGroup(dto.getPracticeGroup())
                .managementClass(dto.getManagementClass())
                .status(ClassSectionStatus.ACTIVE)
                .build()
        ));
  }

  private ClassSchedule upsertClassSchedule(Long classSectionId, MasterTimetableRowDto dto) {
    String weeksPatternJson = toJsonWeeks(dto.getActiveWeeks());

    return classScheduleRepository.findByClassSectionIdAndDayOfWeekAndStartPeriod(
            classSectionId, dto.getDayOfWeek(), dto.getStartPeriod())
        .map(existing -> {
          existing.setNumPeriods(dto.getNumPeriods());
          existing.setStartTime(dto.getStartTime());
          existing.setEndTime(dto.getEndTime());
          existing.setWeeksPattern(weeksPatternJson);
          existing.setRoomText(dto.getRoomText());
          existing.setBuilding(dto.getBuilding());
          existing.setOnline(dto.isOnline());
          return classScheduleRepository.save(existing);
        })
        .orElseGet(() -> classScheduleRepository.save(
            ClassSchedule.builder()
                .classSectionId(classSectionId)
                .dayOfWeek(dto.getDayOfWeek())
                .startPeriod(dto.getStartPeriod())
                .numPeriods(dto.getNumPeriods())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .weeksPattern(weeksPatternJson)
                .roomText(dto.getRoomText())
                .building(dto.getBuilding())
                .isOnline(dto.isOnline())
                .build()
        ));
  }

  private String toJsonWeeks(Set<Integer> weeks) {
    try {
      return objectMapper.writeValueAsString(weeks);
    } catch (JsonProcessingException e) {
      return weeks.toString();
    }
  }

  private Room allocateOnlineRoom(ClassSection section, AtomicInteger onlineRoomsAllocated) {
    Room room = roomRepository.findByClassSectionId(section.getId())
        .orElseGet(() -> {
          onlineRoomsAllocated.incrementAndGet();
          return roomRepository.save(
              Room.builder()
                  .classSectionId(section.getId())
                  .name("Phòng trực tuyến - " + section.getClassCode())
                  .passcode(generatePin())
                  .status(RoomStatus.ACTIVE)
                  .build()
          );
        });
    meetingRoomService.initializeSetting(room);
    return room;
  }

  // Chống nhân đôi session khi re-import
  private int generateOnlineSessions(LocalDate semesterStart, ClassSchedule sched, Room room,
      MasterTimetableRowDto dto) {
    int count = 0;
    Map<LocalDate, MeetingSession> existingSessions = new HashMap<>();
    sessionRepository.findAllByScheduleId(sched.getId())
        .forEach(session -> existingSessions.put(session.getStudyDate(), session));
    for (Integer weekNum : dto.getActiveWeeks()) {
      LocalDate studyDate = dateCalculator.calculateStudyDate(semesterStart, weekNum,
          sched.getDayOfWeek());

      MeetingSession existing = existingSessions.get(studyDate);
      if (existing == null) {
        sessionRepository.save(
            MeetingSession.builder()
                .roomId(room.getId())
                .scheduleId(sched.getId())
                .studyDate(studyDate)
                .scheduledStart(LocalDateTime.of(studyDate, sched.getStartTime()))
                .scheduledEnd(LocalDateTime.of(studyDate, sched.getEndTime()))
                .status(SessionStatus.SCHEDULED)
                .build()
        );
        count++;
      } else if (existing.getStatus() == SessionStatus.SCHEDULED) {
        LocalDateTime start = LocalDateTime.of(studyDate, sched.getStartTime());
        LocalDateTime end = LocalDateTime.of(studyDate, sched.getEndTime());
        if (!Objects.equals(existing.getScheduledStart(), start)
            || !Objects.equals(existing.getScheduledEnd(), end)) {
          existing.setScheduledStart(start);
          existing.setScheduledEnd(end);
          sessionRepository.save(existing);
        }
      }
    }
    return count;
  }

  private String generatePin() {
    return String.valueOf(100000 + new Random().nextInt(900000));
  }
}
