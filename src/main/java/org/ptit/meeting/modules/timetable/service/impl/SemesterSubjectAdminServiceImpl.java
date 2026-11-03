package org.ptit.meeting.modules.timetable.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.ptit.meeting.modules.timetable.dto.request.SemesterSubjectSearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectFilterResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectResponse;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.service.SemesterSubjectAdminService;
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

  private final SemesterRepository semesterRepository;
  private final SubjectRepository subjectRepository;
  private final ClassSectionRepository classSectionRepository;
  private final ClassSchedulerRepository classSchedulerRepository;

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
}
