package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
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
import org.ptit.meeting.modules.timetable.dto.request.SemesterCreateRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.mapper.SemesterMapper;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.impl.SemesterAdminServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SemesterAdminServiceTest {

  @Mock
  private SemesterRepository semesterRepository;

  @Mock
  private SemesterMapper semesterMapper;

  @Mock
  private ScheduleDateCalculator scheduleDateCalculator;

  @InjectMocks
  private SemesterAdminServiceImpl semesterAdminService;

  private Semester semester;
  private SemesterResponse semesterResponse;

  @BeforeEach
  void setUp() {
    // 2026-08-10 is a Monday
    semester = Semester.builder()
        .id(1L)
        .code("2026-2027-HK1")
        .name("Học kỳ 1 năm học 2026 - 2027")
        .academicYear("2026-2027")
        .semesterNum(1)
        .startDate(LocalDate.of(2026, 8, 10))
        .endDate(LocalDate.of(2026, 12, 31))
        .isActive(false)
        .build();

    semesterResponse = new SemesterResponse(
        1L,
        "2026-2027-HK1",
        "Học kỳ 1 năm học 2026 - 2027",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 10),
        LocalDate.of(2026, 12, 31),
        true,
        21L,
        null,
        null
    );
  }

  @Test
  @DisplayName("Create semester successfully with isActive = true")
  void create_Success_WithActive() {
    SemesterCreateRequest request = new SemesterCreateRequest(
        "2026-2027-HK1",
        "Học kỳ 1 năm học 2026 - 2027",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 10),
        LocalDate.of(2026, 12, 31),
        true
    );

    when(semesterRepository.existsByCode("2026-2027-HK1")).thenReturn(false);
    when(semesterRepository.save(any(Semester.class))).thenReturn(semester);

    List<Semester> allSemesters = new ArrayList<>(List.of(semester));
    when(semesterRepository.findAllForUpdate()).thenReturn(allSemesters);
    when(scheduleDateCalculator.calculateTotalWeeks(any(), any())).thenReturn(21L);
    when(semesterMapper.toResponse(any(Semester.class), eq(21L))).thenReturn(semesterResponse);

    SemesterResponse actual = semesterAdminService.create(request);

    assertNotNull(actual);
    assertEquals("2026-2027-HK1", actual.code());
    verify(semesterRepository).findAllForUpdate();
    verify(semesterRepository).saveAll(allSemesters);
  }

  @Test
  @DisplayName("Create semester throws exception when code already exists")
  void create_DuplicateCode_ThrowsException() {
    SemesterCreateRequest request = new SemesterCreateRequest(
        "2026-2027-HK1",
        "Học kỳ 1",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 10),
        LocalDate.of(2026, 12, 31),
        false
    );

    when(semesterRepository.existsByCode("2026-2027-HK1")).thenReturn(true);

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> semesterAdminService.create(request)
    );

    assertEquals(TimetableErrorConstants.SEMESTER_CODE_EXISTED, exception.getStrCode());
    verify(semesterRepository, never()).save(any());
  }

  @Test
  @DisplayName("Create semester throws exception when startDate is not a Monday")
  void create_StartDateNotMonday_ThrowsException() {
    // 2026-08-11 is a Tuesday
    SemesterCreateRequest request = new SemesterCreateRequest(
        "2026-2027-HK1",
        "Học kỳ 1",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 11),
        LocalDate.of(2026, 12, 31),
        false
    );

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> semesterAdminService.create(request)
    );

    assertEquals(TimetableErrorConstants.SEMESTER_START_DATE_NOT_MONDAY, exception.getStrCode());
    verify(semesterRepository, never()).save(any());
  }

  @Test
  @DisplayName("Create semester throws exception when endDate is before startDate")
  void create_EndDateBeforeStartDate_ThrowsException() {
    SemesterCreateRequest request = new SemesterCreateRequest(
        "2026-2027-HK1",
        "Học kỳ 1",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 10),
        LocalDate.of(2026, 8, 9),
        false
    );

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> semesterAdminService.create(request)
    );

    assertEquals(TimetableErrorConstants.SEMESTER_DATE_INVALID, exception.getStrCode());
    verify(semesterRepository, never()).save(any());
  }

  @Test
  @DisplayName("Update semester successfully")
  void update_Success() {
    SemesterUpdateRequest request = new SemesterUpdateRequest(
        "2026-2027-HK1",
        "Học kỳ 1 (Cập nhật)",
        "2026-2027",
        1,
        LocalDate.of(2026, 8, 10),
        LocalDate.of(2026, 12, 31),
        false
    );

    when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
    when(semesterRepository.existsByCodeAndIdNot("2026-2027-HK1", 1L)).thenReturn(false);
    when(semesterRepository.save(any(Semester.class))).thenReturn(semester);
    when(scheduleDateCalculator.calculateTotalWeeks(any(), any())).thenReturn(21L);
    when(semesterMapper.toResponse(any(Semester.class), eq(21L))).thenReturn(semesterResponse);

    SemesterResponse actual = semesterAdminService.update(1L, request);

    assertNotNull(actual);
    assertEquals("Học kỳ 1 (Cập nhật)", semester.getName());
  }

  @Test
  @DisplayName("Activate semester successfully")
  void activate_Success() {
    when(semesterRepository.findAllForUpdate()).thenReturn(new ArrayList<>(List.of(semester)));
    when(scheduleDateCalculator.calculateTotalWeeks(any(), any())).thenReturn(21L);
    when(semesterMapper.toResponse(any(Semester.class), eq(21L))).thenReturn(semesterResponse);

    SemesterResponse actual = semesterAdminService.activate(1L);

    assertNotNull(actual);
    assertTrue(semester.isActive());
  }

  @Test
  @DisplayName("Get active semester throws not found when none is active")
  void getActive_NotFound_ThrowsException() {
    when(semesterRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> semesterAdminService.getActive()
    );

    assertEquals(TimetableErrorConstants.ACTIVE_SEMESTER_NOT_FOUND, exception.getStrCode());
  }

  @Test
  @DisplayName("Search semesters handles null request and returns paged response")
  void search_NullRequest_HandlesGracefully() {
    Page<Semester> page = new PageImpl<>(List.of(semester));
    when(semesterRepository.search(any(), any(), any(Pageable.class))).thenReturn(page);
    when(scheduleDateCalculator.calculateTotalWeeks(any(), any())).thenReturn(21L);
    when(semesterMapper.toResponse(any(Semester.class), eq(21L))).thenReturn(semesterResponse);

    PageResponseDTO<SemesterResponse> result = semesterAdminService.search(null);

    assertNotNull(result);
    assertEquals(1, result.content().size());
  }
}
