package org.ptit.meeting.modules.timetable.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.dto.internal.ExportFileResult;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterSubjectRow;
import org.ptit.meeting.modules.timetable.dto.internal.ClassSectionListRow;
import org.ptit.meeting.modules.timetable.dto.internal.ClassSectionStudentRow;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionStudentSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSubjectSearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectFilterResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSummaryResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectSectionsResponse;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.service.SemesterSubjectAdminService;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionLecturerUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionLecturerResponse;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SemesterSubjectAdminServiceImpl implements SemesterSubjectAdminService {

  private static final int EXPORT_LIMIT = 10_000;
  private static final Comparator<ClassSchedule> SCHEDULE_ORDER = Comparator
      .comparing(ClassSchedule::getDayOfWeek)
      .thenComparing(ClassSchedule::getStartTime)
      .thenComparing(ClassSchedule::getId);

  private final SemesterRepository semesterRepository;
  private final SubjectRepository subjectRepository;
  private final ClassSectionRepository classSectionRepository;
  private final ClassSchedulerRepository classSchedulerRepository;
  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final RoomRepository roomRepository;
  private final UserService userService;
  private final MeetingRoomService meetingRoomService;

  @Override
  @Transactional(readOnly = true)
  public List<ClassSectionLecturerResponse> searchLecturers(Long semesterId, String keyword) {
    getSemester(semesterId);
    return userService.searchLecturers(keyword).stream()
        .map(user -> new ClassSectionLecturerResponse(
            user.getId(), user.getCode(), user.getFullName(), user.getEmail())).toList();
  }

  @Override
  @Transactional
  public ClassSectionDetailResponse assignLecturer(Long sectionId, ClassSectionLecturerUpdateRequest request) {
    ClassSection section = classSectionRepository.findByIdForUpdate(sectionId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.CLASS_SECTION_NOT_FOUND, sectionId));
    if (section.getStatus() != ClassSectionStatus.ACTIVE) {
      throw BusinessException.invalidInput(TimetableErrorConstants.CLASS_SECTION_INACTIVE);
    }
    if (request == null || request.lecturerId() == null || request.lecturerId() <= 0) {
      throw BusinessException.invalidInput(TimetableErrorConstants.LECTURER_INVALID);
    }
    User lecturer = userService.getUserById(request.lecturerId());
    userService.validateUserStatus(lecturer);
    if (lecturer.getAccountType() != AccountType.LECTURER) {
      throw BusinessException.invalidInput(TimetableErrorConstants.LECTURER_INVALID);
    }
    section.setLecturerId(lecturer.getId());
    section.setLecturerName(lecturer.getFullName());
    classSectionRepository.saveAndFlush(section);
    meetingRoomService.refreshClassAccess(sectionId);
    return toClassSectionDetail(section, getSemester(section.getSemesterId()), getSubject(section.getSubjectId()));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<SemesterSubjectResponse> search(
      Long semesterId,
      SemesterSubjectSearchRequest request
  ) {
    Semester semester = getSemester(semesterId);
    Page<SemesterSubjectRow> rows = searchRows(semesterId, request, request.getPage(), request.getSize());
    return PageResponseDTO.from(rows.map(row -> toResponse(row, semester, request.getCohort())));
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterSubjectFilterResponse getFilters(Long semesterId) {
    getSemester(semesterId);
    return new SemesterSubjectFilterResponse(
        semesterId,
        subjectRepository.findDepartmentsBySemesterId(semesterId),
        classSectionRepository.findCohortsBySemesterId(semesterId),
        classSectionRepository.findManagementClassesBySemesterId(semesterId)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterSubjectDetailResponse getDetail(Long semesterId, Long subjectId, String cohort) {
    Semester semester = getSemester(semesterId);
    Subject subject = subjectRepository.findById(subjectId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SUBJECT_NOT_FOUND,
            subjectId
        ));
    List<ClassSection> sections = classSectionRepository
        .findAllBySemesterIdAndSubjectIdAndCohort(semesterId, subjectId, cohort.trim());
    if (sections.isEmpty()) {
      throw BusinessException.notExisted(
          TimetableErrorConstants.SEMESTER_SUBJECT_NOT_FOUND,
          subjectId,
          semesterId
      );
    }

    List<Long> sectionIds = sections.stream().map(ClassSection::getId).toList();
    Map<Long, List<ClassSchedule>> schedules = classSchedulerRepository
        .findAllByClassSectionIdIn(sectionIds).stream()
        .collect(Collectors.groupingBy(ClassSchedule::getClassSectionId));

    SemesterSubjectResponse subjectResponse = new SemesterSubjectResponse(
        subject.getId(), subject.getCode(), subject.getName(), subject.getCredit(),
        semester.getId(), semester.getCode(), semester.getName(), subject.getDepartment(),
        cohort.trim(), sections.size()
    );

    List<SemesterSubjectDetailResponse.ClassSectionDetail> sectionResponses = sections.stream()
        .map(section -> toSectionDetail(section, schedules.getOrDefault(section.getId(), List.of())))
        .toList();
    return new SemesterSubjectDetailResponse(subjectResponse, sectionResponses);
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterSubjectSectionsResponse searchSections(
      Long semesterId,
      Long subjectId,
      ClassSectionSearchRequest request
  ) {
    Semester semester = getSemester(semesterId);
    Subject subject = getSubject(subjectId);
    String cohort = request.getCohort().trim();

    List<ClassSection> subjectSections = classSectionRepository
        .findAllBySemesterIdAndSubjectIdAndCohort(semesterId, subjectId, cohort);
    if (subjectSections.isEmpty()) {
      throw semesterSubjectNotFound(subjectId, semesterId);
    }

    String direction = "DESC".equalsIgnoreCase(request.getDirection()) ? "DESC" : "ASC";
    Page<ClassSectionListRow> sectionPage = classSectionRepository.searchAdminSections(
        semesterId,
        subjectId,
        cohort,
        normalize(request.getKeyword()),
        EnrollmentStatus.ENROLLED,
        normalizeSectionSort(request.getSortBy()),
        direction,
        PageRequest.of(request.getPage(), request.getSize())
    );

    List<Long> sectionIds = sectionPage.getContent().stream().map(ClassSectionListRow::id).toList();
    Map<Long, List<ClassSchedule>> schedules = sectionIds.isEmpty() ? Map.of()
        : classSchedulerRepository.findAllByClassSectionIdIn(sectionIds).stream()
            .collect(Collectors.groupingBy(ClassSchedule::getClassSectionId));
    Map<Long, Room> rooms = sectionIds.isEmpty() ? Map.of()
        : roomRepository.findAllByClassSectionIdIn(sectionIds).stream()
            .collect(Collectors.toMap(Room::getClassSectionId, room -> room));

    Page<ClassSectionSummaryResponse> responses = sectionPage.map(row -> toSectionSummary(
        row,
        schedules.getOrDefault(row.id(), List.of()),
        rooms.get(row.id())
    ));
    SemesterSubjectResponse subjectResponse = toSubjectResponse(
        subject,
        semester,
        cohort,
        subjectSections.size()
    );
    return new SemesterSubjectSectionsResponse(subjectResponse, PageResponseDTO.from(responses));
  }

  @Override
  @Transactional(readOnly = true)
  public ClassSectionDetailResponse getSectionDetail(
      Long semesterId,
      Long subjectId,
      Long sectionId,
      String cohort
  ) {
    Semester semester = getSemester(semesterId);
    Subject subject = getSubject(subjectId);
    ClassSection section = getClassSection(semesterId, subjectId, sectionId, cohort);
    return toClassSectionDetail(section, semester, subject);
  }

  private ClassSectionDetailResponse toClassSectionDetail(ClassSection section, Semester semester, Subject subject) {
    Long sectionId = section.getId();
    long enrolledCount = classEnrollmentRepository.countByClassSectionIdAndStatus(
        sectionId,
        EnrollmentStatus.ENROLLED
    );
    List<ClassSectionDetailResponse.ScheduleDetail> schedules = classSchedulerRepository
        .findAllByClassSectionIdAndIsOnlineTrue(sectionId).stream()
        .sorted(SCHEDULE_ORDER)
        .map(this::toClassSectionScheduleDetail)
        .toList();
    ClassSectionSummaryResponse.OnlineRoomSummary onlineRoom = roomRepository
        .findByClassSectionId(sectionId)
        .map(this::toOnlineRoomSummary)
        .orElse(null);

    return new ClassSectionDetailResponse(
        section.getId(), section.getSemesterId(), semester.getCode(), semester.getName(),
        section.getSubjectId(), subject.getCode(), subject.getName(), subject.getCredit(),
        subject.getDepartment(), section.getClassCode(),
        section.getGroupNumber(), section.getPracticeGroup(), section.getCohort(),
        section.getManagementClass(), section.getLecturerId(), section.getLecturerName(),
        section.getStatus().name(), enrolledCount, section.getExpectedStudentCount(),
        onlineRoom, schedules
    );
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<ClassSectionStudentResponse> searchSectionStudents(
      Long semesterId,
      Long subjectId,
      Long sectionId,
      ClassSectionStudentSearchRequest request
  ) {
    getSemester(semesterId);
    getSubject(subjectId);
    getClassSection(semesterId, subjectId, sectionId, request.getCohort());

    String direction = "DESC".equalsIgnoreCase(request.getDirection()) ? "DESC" : "ASC";
    Page<ClassSectionStudentRow> students = classEnrollmentRepository.searchStudents(
        sectionId,
        EnrollmentStatus.ENROLLED,
        normalize(request.getKeyword()),
        normalizeStudentSort(request.getSortBy()),
        direction,
        PageRequest.of(request.getPage(), request.getSize())
    );
    return PageResponseDTO.from(students.map(this::toStudentResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public ExportFileResult export(Long semesterId, SemesterSubjectSearchRequest request) {
    Semester semester = getSemester(semesterId);
    List<SemesterSubjectResponse> subjects = searchRows(semesterId, request, 0, EXPORT_LIMIT)
        .getContent().stream()
        .map(row -> toResponse(row, semester, request.getCohort()))
        .toList();
    return new ExportFileResult(
        "mon-hoc-" + semester.getCode() + ".xlsx",
        createWorkbook(subjects)
    );
  }

  private Page<SemesterSubjectRow> searchRows(
      Long semesterId,
      SemesterSubjectSearchRequest request,
      int page,
      int size
  ) {
    Sort.Direction direction = "DESC".equalsIgnoreCase(request.getDirection())
        ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, normalizeSort(request.getSortBy())));
    return subjectRepository.searchBySemester(
        semesterId,
        request.getCohort().trim(),
        normalize(request.getDepartment()),
        normalize(request.getManagementClass()),
        normalize(request.getKeyword()),
        pageable
    );
  }

  private SemesterSubjectResponse toResponse(
      SemesterSubjectRow row,
      Semester semester,
      String cohort
  ) {
    return new SemesterSubjectResponse(
        row.subjectId(), row.subjectCode(), row.subjectName(), row.credit(),
        semester.getId(), semester.getCode(), semester.getName(), row.department(),
        cohort.trim(), row.classSectionCount()
    );
  }

  private SemesterSubjectDetailResponse.ClassSectionDetail toSectionDetail(
      ClassSection section,
      List<ClassSchedule> schedules
  ) {
    return new SemesterSubjectDetailResponse.ClassSectionDetail(
        section.getId(), section.getClassCode(), section.getGroupNumber(),
        section.getPracticeGroup(), section.getCohort(), section.getManagementClass(),
        section.getLecturerName(), section.getStatus().name(),
        schedules.stream().map(this::toScheduleDetail).toList()
    );
  }

  private SemesterSubjectDetailResponse.ScheduleDetail toScheduleDetail(ClassSchedule schedule) {
    return new SemesterSubjectDetailResponse.ScheduleDetail(
        schedule.getId(), schedule.getDayOfWeek(), schedule.getStartPeriod(),
        schedule.getNumPeriods(), schedule.getStartTime(), schedule.getEndTime(),
        schedule.getWeeksPattern(), schedule.getRoomText(), schedule.getBuilding(),
        schedule.isOnline()
    );
  }

  private ClassSectionSummaryResponse toSectionSummary(
      ClassSectionListRow row,
      List<ClassSchedule> schedules,
      Room room
  ) {
    ClassSectionSummaryResponse.ScheduleSummary primarySchedule = schedules.stream()
        .sorted(SCHEDULE_ORDER)
        .findFirst()
        .map(this::toScheduleSummary)
        .orElse(null);
    return new ClassSectionSummaryResponse(
        row.id(), row.classCode(), row.groupNumber(), row.practiceGroup(), row.lecturerId(),
        row.lecturerName(), row.status().name(), row.enrolledStudentCount(),
        row.expectedStudentCount(), primarySchedule,
        room == null ? null : toOnlineRoomSummary(room)
    );
  }

  private ClassSectionSummaryResponse.ScheduleSummary toScheduleSummary(ClassSchedule schedule) {
    return new ClassSectionSummaryResponse.ScheduleSummary(
        schedule.getId(), schedule.getDayOfWeek(), schedule.getStartPeriod(),
        schedule.getNumPeriods(), schedule.getStartTime(), schedule.getEndTime(),
        schedule.getRoomText(), schedule.getBuilding(), schedule.isOnline()
    );
  }

  private ClassSectionDetailResponse.ScheduleDetail toClassSectionScheduleDetail(
      ClassSchedule schedule
  ) {
    return new ClassSectionDetailResponse.ScheduleDetail(
        schedule.getId(), schedule.getDayOfWeek(), schedule.getStartPeriod(),
        schedule.getNumPeriods(), schedule.getStartTime(), schedule.getEndTime(),
        schedule.getWeeksPattern(), schedule.getRoomText(), schedule.getBuilding(),
        schedule.isOnline()
    );
  }

  private ClassSectionSummaryResponse.OnlineRoomSummary toOnlineRoomSummary(Room room) {
    return new ClassSectionSummaryResponse.OnlineRoomSummary(
        room.getId(), room.getName(), room.getStatus().name()
    );
  }

  private ClassSectionStudentResponse toStudentResponse(ClassSectionStudentRow row) {
    return new ClassSectionStudentResponse(
        row.enrollmentId(), row.studentId(), row.studentCode(), row.fullName(), row.email(),
        row.managementClass(), row.status().name(), row.enrolledAt()
    );
  }

  private ClassSection getClassSection(
      Long semesterId,
      Long subjectId,
      Long sectionId,
      String cohort
  ) {
    return classSectionRepository.findByIdAndSemesterIdAndSubjectIdAndCohort(
        sectionId,
        semesterId,
        subjectId,
        cohort.trim()
    ).orElseThrow(() -> BusinessException.notExisted(
        TimetableErrorConstants.CLASS_SECTION_NOT_FOUND,
        sectionId
    ));
  }

  private SemesterSubjectResponse toSubjectResponse(
      Subject subject,
      Semester semester,
      String cohort,
      long classSectionCount
  ) {
    return new SemesterSubjectResponse(
        subject.getId(), subject.getCode(), subject.getName(), subject.getCredit(),
        semester.getId(), semester.getCode(), semester.getName(), subject.getDepartment(),
        cohort, classSectionCount
    );
  }

  private byte[] createWorkbook(List<SemesterSubjectResponse> subjects) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Môn học");
      String[] headers = {
          "STT", "Mã môn học", "Tên môn học", "Học kỳ",
          "Khoa/Viện", "Khóa", "Số lớp học phần"
      };
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }
      for (int i = 0; i < subjects.size(); i++) {
        SemesterSubjectResponse subject = subjects.get(i);
        Row row = sheet.createRow(i + 1);
        row.createCell(0).setCellValue(i + 1);
        row.createCell(1).setCellValue(subject.subjectCode());
        row.createCell(2).setCellValue(subject.subjectName());
        row.createCell(3).setCellValue(subject.semesterName());
        row.createCell(4).setCellValue(subject.department() == null ? "" : subject.department());
        row.createCell(5).setCellValue(subject.cohort());
        row.createCell(6).setCellValue(subject.classSectionCount());
      }
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create semester subject export", exception);
    }
  }

  private Semester getSemester(Long semesterId) {
    return semesterRepository.findById(semesterId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SEMESTER_NOT_FOUND,
            semesterId
        ));
  }

  private Subject getSubject(Long subjectId) {
    return subjectRepository.findById(subjectId)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SUBJECT_NOT_FOUND,
            subjectId
        ));
  }

  private BusinessException semesterSubjectNotFound(Long subjectId, Long semesterId) {
    return BusinessException.notExisted(
        TimetableErrorConstants.SEMESTER_SUBJECT_NOT_FOUND,
        subjectId,
        semesterId
    );
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeSort(String sortBy) {
    if (sortBy == null) {
      return "code";
    }
    return switch (sortBy.toLowerCase(Locale.ROOT)) {
      case "name" -> "name";
      default -> "code";
    };
  }

  private String normalizeSectionSort(String sortBy) {
    if (sortBy == null) {
      return "classCode";
    }
    return switch (sortBy) {
      case "groupNumber", "lecturerName", "enrolledStudentCount", "schedule", "room" -> sortBy;
      default -> "classCode";
    };
  }

  private String normalizeStudentSort(String sortBy) {
    if (sortBy == null) {
      return "studentCode";
    }
    return switch (sortBy) {
      case "fullName", "managementClass", "enrolledAt" -> sortBy;
      default -> "studentCode";
    };
  }
}
