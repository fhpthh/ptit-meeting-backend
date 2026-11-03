package org.ptit.meeting.modules.timetable.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.timetable.mapper.SemesterMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.dto.request.SemesterCreateRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterResponse;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterClassSectionCountRow;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.ImportHistoryRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.ScheduleDateCalculator;
import org.ptit.meeting.modules.timetable.service.SemesterAdminService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterAdminServiceImpl implements SemesterAdminService {

  private final SemesterRepository semesterRepository;
  private final ClassSectionRepository classSectionRepository;
  private final ImportHistoryRepository importHistoryRepository;
  private final SemesterMapper semesterMapper;
  private final ScheduleDateCalculator scheduleDateCalculator;

  @Override
  @Transactional
  public SemesterResponse create(SemesterCreateRequest request) {
    validateDates(request.startDate(), request.endDate());

    String code = request.code().trim();

    if (semesterRepository.existsByCode(code)) {
      throw BusinessException.existed(
          TimetableErrorConstants.SEMESTER_CODE_EXISTED,
          code
      );
    }

    Semester semester = Semester.builder()
        .code(code)
        .name(request.name().trim())
        .academicYear(request.academicYear().trim())
        .semesterNum(request.semesterNum())
        .startDate(request.startDate())
        .endDate(request.endDate())
        .isActive(false)
        .build();

    semester = semesterRepository.save(semester);

    if (request.isActive()) {
      semester = activateLocked(semester.getId());
    }

    return toResponse(semester, 0L);
  }

  @Override
  @Transactional
  public SemesterResponse update(Long id, SemesterUpdateRequest request) {
    validateDates(request.startDate(), request.endDate());

    Semester semester = getEntity(id);
    String code = request.code().trim();

    if (semesterRepository.existsByCodeAndIdNot(code, id)) {
      throw BusinessException.existed(
          TimetableErrorConstants.SEMESTER_CODE_EXISTED,
          code
      );
    }

    semester.setCode(code);
    semester.setName(request.name().trim());
    semester.setAcademicYear(request.academicYear().trim());
    semester.setSemesterNum(request.semesterNum());
    semester.setStartDate(request.startDate());
    semester.setEndDate(request.endDate());

    semester.setActive(false);
    semester = semesterRepository.save(semester);

    if (request.isActive()) {
      semester = activateLocked(semester.getId());
    }

    return toResponse(semester);
  }

  @Override
  @Transactional
  public SemesterResponse activate(Long id) {
    return toResponse(activateLocked(id));
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterResponse getById(Long id) {
    return toResponse(getEntity(id));
  }

  @Override
  @Transactional(readOnly = true)
  public SemesterResponse getActive() {
    Semester semester = semesterRepository.findByIsActiveTrue()
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.ACTIVE_SEMESTER_NOT_FOUND
        ));

    return toResponse(semester);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Semester semester = getEntity(id);

    if (semester.isActive()) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.SEMESTER_ACTIVE_DELETE_FORBIDDEN,
          id
      );
    }

    boolean hasRelatedData = classSectionRepository.existsBySemesterId(id)
        || importHistoryRepository.existsBySemesterId(id);

    if (hasRelatedData) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.SEMESTER_HAS_RELATED_DATA,
          id
      );
    }

    semesterRepository.delete(semester);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<SemesterResponse> search(SemesterSearchRequest request) {
    if (request == null) {
      request = new SemesterSearchRequest();
    }

    String keyword = normalize(request.getKeyword());
    String academicYear = normalize(request.getAcademicYear());

    Sort.Direction direction = parseDirection(request.getDirection());

    PageRequest pageable = PageRequest.of(
        Math.max(request.getPage(), 0),
        Math.min(Math.max(request.getSize(), 1), 100),
        Sort.by(direction, normalizeSortField(request.getSortBy()))
    );

    Page<Semester> semesterPage = semesterRepository.search(keyword, academicYear, pageable);
    Map<Long, Long> classSectionCounts = getClassSectionCounts(semesterPage.getContent());

    Page<SemesterResponse> page = semesterPage.map(semester ->
        toResponse(semester, classSectionCounts.getOrDefault(semester.getId(), 0L))
    );

    return PageResponseDTO.from(page);
  }

  private Semester activateLocked(Long id) {
    List<Semester> semesters = semesterRepository.findAllForUpdate();

    Semester target = semesters.stream()
        .filter(item -> item.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SEMESTER_NOT_FOUND,
            id
        ));

    for (Semester semester : semesters) {
      semester.setActive(semester.getId().equals(id));
    }

    semesterRepository.saveAll(semesters);
    return target;
  }

  private Semester getEntity(Long id) {
    return semesterRepository.findById(id)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.SEMESTER_NOT_FOUND,
            id
        ));
  }

  private SemesterResponse toResponse(Semester semester) {
    long classSectionCount = classSectionRepository.countBySemesterIds(Set.of(semester.getId()))
        .stream()
        .findFirst()
        .map(SemesterClassSectionCountRow::classSectionCount)
        .orElse(0L);

    return toResponse(semester, classSectionCount);
  }

  private SemesterResponse toResponse(Semester semester, long classSectionCount) {
    long totalWeeks = scheduleDateCalculator.calculateTotalWeeks(
        semester.getStartDate(),
        semester.getEndDate()
    );

    return semesterMapper.toResponse(semester, totalWeeks, classSectionCount);
  }

  private Map<Long, Long> getClassSectionCounts(List<Semester> semesters) {
    if (semesters.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<Long> semesterIds = semesters.stream()
        .map(Semester::getId)
        .collect(Collectors.toSet());

    return classSectionRepository.countBySemesterIds(semesterIds).stream()
        .collect(Collectors.toMap(
            SemesterClassSectionCountRow::semesterId,
            SemesterClassSectionCountRow::classSectionCount
        ));
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (!endDate.isAfter(startDate)) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.SEMESTER_DATE_INVALID
      );
    }

    if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
      throw BusinessException.invalidInput(
          TimetableErrorConstants.SEMESTER_START_DATE_NOT_MONDAY
      );
    }
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String normalizeSortField(String value) {
    if (value == null || value.isBlank()) {
      return "startDate";
    }
    return switch (value) {
      case "code", "name", "academicYear", "startDate", "endDate",
           "createdAt", "updatedAt", "semesterNum" -> value;
      default -> "startDate";
    };
  }

  private Sort.Direction parseDirection(String value) {
    return "ASC".equalsIgnoreCase(value)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;
  }
}
