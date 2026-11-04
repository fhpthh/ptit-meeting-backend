package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.dto.internal.ClassSectionListRow;
import org.ptit.meeting.modules.timetable.dto.internal.ExportFileResult;
import org.ptit.meeting.modules.timetable.dto.internal.SemesterSubjectRow;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSubjectSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionSearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectFilterResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectSectionsResponse;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.service.impl.SemesterSubjectAdminServiceImpl;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SemesterSubjectAdminServiceTest {

  @Mock
  private SemesterRepository semesterRepository;

  @Mock
  private SubjectRepository subjectRepository;

  @Mock
  private ClassSectionRepository classSectionRepository;

  @Mock
  private ClassSchedulerRepository classSchedulerRepository;

  @Mock
  private ClassEnrollmentRepository classEnrollmentRepository;

  @Mock
  private RoomRepository roomRepository;

  @InjectMocks
  private SemesterSubjectAdminServiceImpl service;

  private Semester semester;
  private SemesterSubjectSearchRequest request;

  @BeforeEach
  void setUp() {
    semester = Semester.builder()
        .id(1L)
        .code("2026_HK1")
        .name("Học kỳ 1 năm học 2026-2027")
        .academicYear("2026-2027")
        .semesterNum(1)
        .startDate(LocalDate.of(2026, 8, 10))
        .endDate(LocalDate.of(2026, 12, 31))
        .build();
    request = new SemesterSubjectSearchRequest();
    request.setCohort("D22");
  }

  @Test
  @DisplayName("Search semester subjects returns data calculated by the repository")
  void search_ReturnsPagedSubjects() {
    SemesterSubjectRow row = new SemesterSubjectRow(
        10L, "INT1448", "Phát triển phần mềm hướng dịch vụ", 3, "Khoa CNTT", 6L
    );
    when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
    when(subjectRepository.searchBySemester(
        eq(1L), eq("D22"), eq(null), eq(null), eq(null), any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(row)));

    PageResponseDTO<SemesterSubjectResponse> result = service.search(1L, request);

    assertEquals(1, result.content().size());
    assertEquals("INT1448", result.content().getFirst().subjectCode());
    assertEquals(6L, result.content().getFirst().classSectionCount());
    assertEquals("D22", result.content().getFirst().cohort());
  }

  @Test
  @DisplayName("Get filters returns only values available in the selected semester")
  void getFilters_ReturnsSemesterValues() {
    when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
    when(subjectRepository.findDepartmentsBySemesterId(1L)).thenReturn(List.of("Khoa CNTT"));
    when(classSectionRepository.findCohortsBySemesterId(1L)).thenReturn(List.of("D22"));
    when(classSectionRepository.findManagementClassesBySemesterId(1L))
        .thenReturn(List.of("D22CQCN01-B"));

    SemesterSubjectFilterResponse result = service.getFilters(1L);

    assertEquals(List.of("Khoa CNTT"), result.departments());
    assertEquals(List.of("D22"), result.cohorts());
    assertEquals(List.of("D22CQCN01-B"), result.managementClasses());
  }

  @Test
  @DisplayName("Search rejects a semester that does not exist")
  void search_MissingSemester_ThrowsBusinessException() {
    when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> service.search(99L, request)
    );

    assertEquals(TimetableErrorConstants.SEMESTER_NOT_FOUND, exception.getStrCode());
  }

  @Test
  @DisplayName("Export creates a non-empty Excel file from filtered data")
  void export_ReturnsExcelFile() {
    SemesterSubjectRow row = new SemesterSubjectRow(
        10L, "INT1448", "Phát triển phần mềm hướng dịch vụ", 3, "Khoa CNTT", 6L
    );
    when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
    when(subjectRepository.searchBySemester(
        eq(1L), eq("D22"), eq(null), eq(null), eq(null), any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(row)));

    ExportFileResult result = service.export(1L, request);

    assertEquals("mon-hoc-2026_HK1.xlsx", result.fileName());
    assertTrue(result.content().length > 0);
    verify(subjectRepository).searchBySemester(
        eq(1L), eq("D22"), eq(null), eq(null), eq(null), any(Pageable.class)
    );
  }

  @Test
  @DisplayName("Search class sections returns enrollment and expected student counts")
  void searchSections_ReturnsPagedClassSections() {
    ClassSectionSearchRequest sectionRequest = new ClassSectionSearchRequest();
    sectionRequest.setCohort("D22");
    Subject subject = Subject.builder()
        .id(10L)
        .code("INT1448")
        .name("Phat trien phan mem huong dich vu")
        .credit(3)
        .build();
    ClassSection section = ClassSection.builder()
        .id(20L)
        .semesterId(1L)
        .subjectId(10L)
        .classCode("INT1448_01")
        .cohort("D22")
        .status(ClassSectionStatus.ACTIVE)
        .build();
    ClassSectionListRow row = new ClassSectionListRow(
        20L, 1L, 10L, 30L, "Nguyen Van A", "D22", "INT1448_01", "01",
        null, null, ClassSectionStatus.ACTIVE, 70, 68L
    );
    when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
    when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
    when(classSectionRepository.findAllBySemesterIdAndSubjectIdAndCohort(1L, 10L, "D22"))
        .thenReturn(List.of(section));
    when(classSectionRepository.searchAdminSections(
        eq(1L), eq(10L), eq("D22"), eq(null), eq(EnrollmentStatus.ENROLLED),
        eq("classCode"), eq("ASC"), any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(row)));
    when(classSchedulerRepository.findAllByClassSectionIdIn(List.of(20L)))
        .thenReturn(List.of());
    when(roomRepository.findAllByClassSectionIdIn(List.of(20L))).thenReturn(List.of());

    SemesterSubjectSectionsResponse result = service.searchSections(1L, 10L, sectionRequest);

    assertEquals(1, result.classSections().totalElements());
    assertEquals(68L, result.classSections().content().getFirst().enrolledStudentCount());
    assertEquals(70, result.classSections().content().getFirst().expectedStudentCount());
  }
}
