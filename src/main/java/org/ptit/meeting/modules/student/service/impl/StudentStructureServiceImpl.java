package org.ptit.meeting.modules.student.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;
import org.ptit.meeting.modules.student.dto.request.AcademicCatalogCreateRequest;
import org.ptit.meeting.modules.student.dto.request.AdministrativeClassCreateRequest;
import org.ptit.meeting.modules.student.dto.response.AcademicOptionResponse;
import org.ptit.meeting.modules.student.dto.response.AdministrativeClassOptionResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureOptionsResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse.CampusNode;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse.ClassNode;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse.CohortNode;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse.MajorNode;
import org.ptit.meeting.modules.student.entity.AcademicCampus;
import org.ptit.meeting.modules.student.entity.AcademicMajor;
import org.ptit.meeting.modules.student.entity.AdministrativeClass;
import org.ptit.meeting.modules.student.entity.AdmissionCohort;
import org.ptit.meeting.modules.student.repository.AcademicCampusRepository;
import org.ptit.meeting.modules.student.repository.AcademicMajorRepository;
import org.ptit.meeting.modules.student.repository.AdministrativeClassRepository;
import org.ptit.meeting.modules.student.repository.AdmissionCohortRepository;
import org.ptit.meeting.modules.student.repository.StudentAcademicProfileRepository;
import org.ptit.meeting.modules.student.service.StudentStructureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStructureServiceImpl implements StudentStructureService {
  private final AcademicCampusRepository campuses;
  private final AdmissionCohortRepository cohorts;
  private final AcademicMajorRepository majors;
  private final AdministrativeClassRepository administrativeClasses;
  private final StudentAcademicProfileRepository profiles;

  @Override
  public StudentStructureOptionsResponse options(Long campusId, Long cohortId, Long majorId) {
    List<AdministrativeClass> activeClasses = administrativeClasses.findAllByActiveTrueOrderByCodeAsc();
    List<AcademicOptionResponse> cohortOptions = activeClasses.stream()
        .filter(item -> campusId == null || item.getCampus().getId().equals(campusId))
        .map(AdministrativeClass::getCohort).distinct()
        .sorted(Comparator.comparing(AdmissionCohort::getCode).reversed()).map(this::option).toList();
    List<AcademicOptionResponse> majorOptions = activeClasses.stream()
        .filter(item -> campusId == null || item.getCampus().getId().equals(campusId))
        .filter(item -> cohortId == null || item.getCohort().getId().equals(cohortId))
        .map(AdministrativeClass::getMajor).distinct()
        .sorted(Comparator.comparing(AcademicMajor::getName)).map(this::option).toList();
    List<AdministrativeClassOptionResponse> classOptions = activeClasses.stream()
        .filter(item -> campusId == null || item.getCampus().getId().equals(campusId))
        .filter(item -> cohortId == null || item.getCohort().getId().equals(cohortId))
        .filter(item -> majorId == null || item.getMajor().getId().equals(majorId))
        .map(this::classOption).toList();
    return new StudentStructureOptionsResponse(
        campuses.findAllByActiveTrueOrderByNameAsc().stream().map(this::option).toList(),
        cohortOptions, majorOptions,
        classOptions, List.of(StudyStatus.values()));
  }

  @Override
  public StudentStructureTreeResponse tree(String keyword) {
    String normalized = keyword == null || keyword.isBlank()
        ? null : keyword.strip().toLowerCase(Locale.ROOT);
    Map<Long, Long> counts = profiles.countStudentsByAdministrativeClass().stream()
        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    List<AdministrativeClass> classes = administrativeClasses.findAllByActiveTrueOrderByCodeAsc()
        .stream().filter(item -> matches(item, normalized)).toList();
    Map<Long, List<AdministrativeClass>> byCampus = classes.stream()
        .collect(Collectors.groupingBy(item -> item.getCampus().getId()));
    List<CampusNode> campusNodes = new ArrayList<>();
    for (AcademicCampus campus : campuses.findAllByActiveTrueOrderByNameAsc()) {
      List<AdministrativeClass> campusClasses = byCampus.getOrDefault(campus.getId(), List.of());
      if (campusClasses.isEmpty()) {
        continue;
      }
      campusNodes.add(new CampusNode(campus.getId(), campus.getCode(), campus.getName(),
          total(campusClasses, counts), cohortNodes(campusClasses, counts)));
    }
    return new StudentStructureTreeResponse(campusNodes, profiles.countAssignedStudents());
  }

  @Override
  @Transactional
  public AcademicOptionResponse createCampus(AcademicCatalogCreateRequest request) {
    String code = normalizeCode(request.code());
    if (campuses.findByCodeIgnoreCase(code).isPresent()) {
      throw BusinessException.existed("error.student-structure.campus.code-exists");
    }
    return option(campuses.save(AcademicCampus.builder().code(code)
        .name(request.name().strip()).build()));
  }

  @Override
  @Transactional
  public AcademicOptionResponse createCohort(AcademicCatalogCreateRequest request) {
    String code = normalizeCode(request.code());
    if (cohorts.findByCodeIgnoreCase(code).isPresent()) {
      throw BusinessException.existed("error.student-structure.cohort.code-exists");
    }
    return option(cohorts.save(AdmissionCohort.builder().code(code)
        .name(request.name().strip()).build()));
  }

  @Override
  @Transactional
  public AcademicOptionResponse createMajor(AcademicCatalogCreateRequest request) {
    String code = normalizeCode(request.code());
    if (majors.findByCodeIgnoreCase(code).isPresent()) {
      throw BusinessException.existed("error.student-structure.major.code-exists");
    }
    return option(majors.save(AcademicMajor.builder().code(code)
        .name(request.name().strip()).build()));
  }

  @Override
  @Transactional
  public AdministrativeClassOptionResponse createAdministrativeClass(
      AdministrativeClassCreateRequest request) {
    String code = normalizeCode(request.code());
    if (administrativeClasses.findByCodeIgnoreCase(code).isPresent()) {
      throw BusinessException.existed("error.student-structure.class.code-exists");
    }
    AcademicCampus campus = campuses.findById(request.campusId()).filter(AcademicCampus::isActive)
        .orElseThrow(() -> missing("campus", request.campusId()));
    AdmissionCohort cohort = cohorts.findById(request.cohortId()).filter(AdmissionCohort::isActive)
        .orElseThrow(() -> missing("cohort", request.cohortId()));
    AcademicMajor major = majors.findById(request.majorId()).filter(AcademicMajor::isActive)
        .orElseThrow(() -> missing("major", request.majorId()));
    return classOption(administrativeClasses.save(AdministrativeClass.builder().code(code)
        .name(request.name().strip()).campus(campus).cohort(cohort).major(major).build()));
  }

  private List<CohortNode> cohortNodes(List<AdministrativeClass> classes, Map<Long, Long> counts) {
    return classes.stream().collect(Collectors.groupingBy(item -> item.getCohort().getId())).values()
        .stream().map(items -> new CohortNode(items.get(0).getCohort().getId(),
            items.get(0).getCohort().getCode(), items.get(0).getCohort().getName(),
            total(items, counts), majorNodes(items, counts)))
        .sorted(Comparator.comparing(CohortNode::code).reversed()).toList();
  }

  private List<MajorNode> majorNodes(List<AdministrativeClass> classes, Map<Long, Long> counts) {
    return classes.stream().collect(Collectors.groupingBy(item -> item.getMajor().getId())).values()
        .stream().map(items -> new MajorNode(items.get(0).getMajor().getId(),
            items.get(0).getMajor().getCode(), items.get(0).getMajor().getName(),
            total(items, counts), items.stream().map(item -> new ClassNode(item.getId(), item.getCode(),
                item.getName(), counts.getOrDefault(item.getId(), 0L))).toList()))
        .sorted(Comparator.comparing(MajorNode::name)).toList();
  }

  private long total(List<AdministrativeClass> classes, Map<Long, Long> counts) {
    return classes.stream().mapToLong(item -> counts.getOrDefault(item.getId(), 0L)).sum();
  }

  private boolean matches(AdministrativeClass item, String keyword) {
    if (keyword == null) {
      return true;
    }
    return item.getCode().toLowerCase(Locale.ROOT).contains(keyword)
        || item.getName().toLowerCase(Locale.ROOT).contains(keyword)
        || item.getCampus().getName().toLowerCase(Locale.ROOT).contains(keyword)
        || item.getCohort().getCode().toLowerCase(Locale.ROOT).contains(keyword)
        || item.getMajor().getName().toLowerCase(Locale.ROOT).contains(keyword);
  }

  private BusinessException missing(String type, Long id) {
    return BusinessException.notExisted("error.student-structure." + type + ".not-found", id);
  }

  private String normalizeCode(String code) {
    return code.strip().toUpperCase(Locale.ROOT);
  }

  private AcademicOptionResponse option(AcademicCampus item) {
    return new AcademicOptionResponse(item.getId(), item.getCode(), item.getName());
  }

  private AcademicOptionResponse option(AdmissionCohort item) {
    return new AcademicOptionResponse(item.getId(), item.getCode(), item.getName());
  }

  private AcademicOptionResponse option(AcademicMajor item) {
    return new AcademicOptionResponse(item.getId(), item.getCode(), item.getName());
  }

  private AdministrativeClassOptionResponse classOption(AdministrativeClass item) {
    return new AdministrativeClassOptionResponse(item.getId(), item.getCode(), item.getName(),
        item.getCampus().getId(), item.getCohort().getId(), item.getMajor().getId());
  }
}
