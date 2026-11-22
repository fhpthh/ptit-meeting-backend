package org.ptit.meeting.modules.timetable.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSummaryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSessionResponse;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.service.ClassSectionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassSectionServiceImpl implements ClassSectionService {

  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final SemesterRepository semesterRepository;
  private final ClassSectionRepository classSectionRepository;
  private final ClassSchedulerRepository classSchedulerRepository;
  private final MeetingSessionRepository meetingSessionRepository;
  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final SubjectRepository subjectRepository;
  private final RoomRepository roomRepository;

  @Override
  public List<ClassSectionSemesterResponse> getSemesters(Long userId) {
    return semesterRepository
        .findAllForOnlineClasses(userId, ClassSectionStatus.CANCELED, EnrollmentStatus.ENROLLED)
        .stream()
        .map(semester -> new ClassSectionSemesterResponse(
            semester.getId(), semester.getName(), semester.getAcademicYear(),
            semester.getSemesterNum(), semester.isActive()
        )).toList();
  }

  @Override
  public List<ClassSectionResponse> getOnlineClasses(
      Long userId, Long semesterId, String academicYear
  ) {
    String year = academicYear == null || academicYear.isBlank() ? null : academicYear.trim();
    return classSectionRepository.findOnlineClasses(
        userId, semesterId, year, ClassSectionStatus.CANCELED,
        EnrollmentStatus.ENROLLED, SessionStatus.CANCELLED, SessionStatus.ENDED,
        SessionStatus.SCHEDULED, LocalDateTime.now(SCHEDULE_ZONE)
    );
  }

  @Override
  public ClassSectionDetailResponse getDetail(Long userId, Long sectionId) {
    ClassSection section = requireAccessibleClass(userId, sectionId);
    var semester = semesterRepository.findById(section.getSemesterId())
        .orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.SEMESTER_NOT_FOUND));
    var subject = subjectRepository.findById(section.getSubjectId())
        .orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.SUBJECT_NOT_FOUND));
    var schedules = classSchedulerRepository.findAllByClassSectionIdAndIsOnlineTrue(sectionId)
        .stream()
        .sorted(Comparator.comparing(ClassSchedule::getDayOfWeek)
            .thenComparing(ClassSchedule::getStartTime)
            .thenComparing(ClassSchedule::getId))
        .map(schedule -> new ClassSectionDetailResponse.ScheduleDetail(
            schedule.getId(), schedule.getDayOfWeek(), schedule.getStartPeriod(),
            schedule.getNumPeriods(), schedule.getStartTime(), schedule.getEndTime(),
            schedule.getWeeksPattern(), schedule.getRoomText(), schedule.getBuilding(), true
        )).toList();
    var onlineRoom = roomRepository.findByClassSectionId(sectionId)
        .map(room -> new ClassSectionSummaryResponse.OnlineRoomSummary(
            room.getId(), room.getName(), room.getStatus().name(), room.getPublicId()
        )).orElse(null);
    return new ClassSectionDetailResponse(
        sectionId, semester.getId(), semester.getCode(), semester.getName(),
        subject.getId(), subject.getCode(), subject.getName(), subject.getCredit(),
        subject.getDepartment(), section.getClassCode(), section.getGroupNumber(),
        section.getPracticeGroup(), section.getCohort(), section.getManagementClass(),
        section.getLecturerId(), section.getLecturerName(), section.getStatus().name(),
        classEnrollmentRepository.countByClassSectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED),
        section.getExpectedStudentCount(), onlineRoom, schedules
    );
  }

  @Override
  public ClassSessionResponse getSession(Long userId, Long sectionId, Long sessionId) {
    requireAccessibleClass(userId, sectionId);
    return meetingSessionRepository.findClassSession(sectionId, sessionId)
        .orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.CLASS_SESSION_NOT_FOUND));
  }

  @Override
  public PageResponseDTO<ClassSessionResponse> getSessions(
      Long userId, Long sectionId, SessionStatus status, int page, int size
  ) {
    requireAccessibleClass(userId, sectionId);
    return PageResponseDTO.from(meetingSessionRepository.findClassSessions(
        sectionId, status, PageRequest.of(page, size)
    ));
  }

  @Override
  public PageResponseDTO<ClassSectionStudentResponse> getStudents(
      Long userId, Long sectionId, String keyword, String sortBy, int page, int size
  ) {
    requireAccessibleClass(userId, sectionId);
    String search = keyword == null || keyword.isBlank() ? null : keyword.trim();
    String sort = "fullName".equals(sortBy) ? "fullName" : "studentCode";
    var students = classEnrollmentRepository.searchStudents(
        sectionId, EnrollmentStatus.ENROLLED, search, sort, "ASC", PageRequest.of(page, size)
    );
    return PageResponseDTO.from(students.map(row -> new ClassSectionStudentResponse(
        row.enrollmentId(), row.studentId(), row.studentCode(), row.fullName(), row.email(),
        row.managementClass(), row.status().name(), row.enrolledAt()
    )));
  }

  private ClassSection requireAccessibleClass(Long userId, Long sectionId) {
    return classSectionRepository.findAccessibleOnlineClass(
        sectionId, userId, ClassSectionStatus.CANCELED, EnrollmentStatus.ENROLLED
    ).orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.CLASS_SECTION_NOT_FOUND));
  }
}
