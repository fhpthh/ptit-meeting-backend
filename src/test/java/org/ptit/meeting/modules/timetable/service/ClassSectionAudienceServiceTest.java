package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.service.impl.ClassSectionAudienceServiceImpl;

@ExtendWith(MockitoExtension.class)
class ClassSectionAudienceServiceTest {

  @Mock
  private ClassSectionRepository classSectionRepository;
  @Mock
  private ClassEnrollmentRepository classEnrollmentRepository;
  @Mock
  private UserService userService;

  private ClassSectionAudienceServiceImpl audienceService;

  @BeforeEach
  void setUp() {
    audienceService = new ClassSectionAudienceServiceImpl(
        classSectionRepository,
        classEnrollmentRepository,
        userService
    );
  }

  @Test
  void findOwnedClassSectionStudentIds_whenOwnedAndActive_returnsStudentIds() {
    ClassSection section = ClassSection.builder()
        .id(10L)
        .lecturerId(50L)
        .status(ClassSectionStatus.ACTIVE)
        .build();
    when(classSectionRepository.findAllById(Set.of(10L))).thenReturn(List.of(section));
    when(classEnrollmentRepository.findActiveEnrolledStudentUserIdsByClassSectionIds(
        Set.of(10L), EnrollmentStatus.ENROLLED, AccountType.STUDENT, UserStatus.ACTIVE
    )).thenReturn(List.of(101L, 102L));

    Set<Long> result = audienceService.findOwnedClassSectionStudentIds(50L, List.of(10L));

    assertEquals(Set.of(101L, 102L), result);
  }

  @Test
  void findOwnedClassSectionStudentIds_whenNotOwnedByLecturer_throwsUnauthorized() {
    ClassSection section = ClassSection.builder()
        .id(10L)
        .lecturerId(99L)
        .status(ClassSectionStatus.ACTIVE)
        .build();
    when(classSectionRepository.findAllById(Set.of(10L))).thenReturn(List.of(section));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> audienceService.findOwnedClassSectionStudentIds(50L, List.of(10L)));
    assertEquals(TimetableErrorConstants.LECTURER_SEMESTER_ACCESS_DENIED, ex.getStrCode());
  }

  @Test
  void findActiveMemberUserIds_whenSectionMissing_throwsNotFound() {
    when(classSectionRepository.findAllById(Set.of(999L))).thenReturn(List.of());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> audienceService.findActiveMemberUserIds(List.of(999L)));
    assertEquals(TimetableErrorConstants.CLASS_SECTION_NOT_FOUND, ex.getStrCode());
  }
}
