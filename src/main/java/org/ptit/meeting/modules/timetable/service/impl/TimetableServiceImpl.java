package org.ptit.meeting.modules.timetable.service.impl;

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
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.RoomStatus;
import org.ptit.meeting.modules.timetable.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.entity.MeetingSession;
import org.ptit.meeting.modules.timetable.entity.Room;
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
  private final UserRepository userRepository;
  private final ScheduleDateCalculator dateCalculator;

  @Override
  @Transactional
  public TimetableProcessResult processMasterTimetable(Long semesterId, List<MasterTimetableRowDto> rows,
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
        processSingleRow(semester, row, processedSubjects, processedClassSections, onlineRoomsAllocated, meetingSessionsGenerated);
        successCount++;
        schedulesSaved++;
      } catch (Exception e) {
        log.warn("(processMasterTimetable) Error processing row {}: {}", row.getRowNumber(), e.getMessage());
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

    ClassSchedule schedule = saveClassSchedule(section.getId(), dto);

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
    Long lecturerId = findLecturerId(dto.getLecturerName());
    return classSectionRepository.findByClassCodeAndSemesterId(dto.getClassCode(), semesterId)
        .orElseGet(() -> classSectionRepository.save(
            ClassSection.builder()
                .semesterId(semesterId)
                .subjectId(subjectId)
                .lecturerId(lecturerId)
                .lecturerName(dto.getLecturerName())
                .cohort(dto.getCohort())
                .classCode(dto.getClassCode())
                .groupNumber(dto.getGroupNumber())
                .practiceGroup(dto.getPracticeGroup())
                .managementClass(dto.getManagementClass())
                .status(ClassSectionStatus.ACTIVE)
                .build()
        ));
  }

  private Long findLecturerId(String lecturerName) {
    if (lecturerName == null || lecturerName.isBlank()) {
      return null;
    }
    return userRepository.findByEmailOrCode(lecturerName, lecturerName)
        .map(User::getId)
        .orElse(null);
  }

  private ClassSchedule saveClassSchedule(Long classSectionId, MasterTimetableRowDto dto) {
    return classScheduleRepository.save(
        ClassSchedule.builder()
            .classSectionId(classSectionId)
            .dayOfWeek(dto.getDayOfWeek())
            .startPeriod(dto.getStartPeriod())
            .numPeriods(dto.getNumPeriods())
            .startTime(dto.getStartTime())
            .endTime(dto.getEndTime())
            .weeksPattern(dto.getActiveWeeks().toString())
            .roomText(dto.getRoomText())
            .building(dto.getBuilding())
            .isOnline(dto.isOnline())
            .build()
    );
  }

  private Room allocateOnlineRoom(ClassSection section, AtomicInteger onlineRoomsAllocated) {
    return roomRepository.findByClassSectionId(section.getId())
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
  }

  private int generateOnlineSessions(LocalDate semesterStart, ClassSchedule sched, Room room,
      MasterTimetableRowDto dto) {
    int count = 0;
    for (Integer weekNum : dto.getActiveWeeks()) {
      LocalDate studyDate = dateCalculator.calculateStudyDate(semesterStart, weekNum,
          sched.getDayOfWeek());

      if (!sessionRepository.existsByScheduleIdAndStudyDate(sched.getId(), studyDate)) {
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
      }
    }
    return count;
  }

  private String generatePin() {
    return String.valueOf(100000 + new Random().nextInt(900000));
  }
}