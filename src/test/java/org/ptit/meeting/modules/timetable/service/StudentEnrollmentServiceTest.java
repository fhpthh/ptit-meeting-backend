package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.timetable.dto.internal.StudentEnrollmentRowDto;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.service.impl.StudentEnrollmentServiceImpl;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentServiceTest {

  @Mock
  private ClassSectionRepository classSectionRepository;

  @Mock
  private ClassEnrollmentRepository enrollmentRepository;

  @Mock
  private UserService userService;

  @InjectMocks
  private StudentEnrollmentServiceImpl studentEnrollmentService;

  @Test
  @DisplayName("processStudentEnrollments: Ném lỗi nếu lớp học phần không tồn tại")
  void testSectionNotFound() {
    when(classSectionRepository.findById(999L)).thenReturn(Optional.empty());

    List<ImportRowError> errors = new ArrayList<>();
    assertThrows(BusinessException.class, () ->
        studentEnrollmentService.processStudentEnrollments(999L, List.of(), errors)
    );
  }

  @Test
  @DisplayName("processStudentEnrollments: Tạo tài khoản và ghi danh thành công")
  void testProcessEnrollmentsSuccess() {
    Long sectionId = 1L;
    ClassSection section = ClassSection.builder()
        .id(sectionId)
        .classCode("INT1306_01")
        .semesterId(10L)
        .build();

    when(classSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

    StudentEnrollmentRowDto studentDto = StudentEnrollmentRowDto.builder()
        .studentCode("B22DCCN356")
        .fullName("Phan Thị Hồng Huế")
        .managementClass("D22CN08")
        .build();

    User mockUser = User.builder()
        .id(32L)
        .code("B22DCCN356")
        .fullName("Phan Thị Hồng Huế")
        .email("HuePTH.B22CN356@stu.ptit.edu.vn")
        .accountType(AccountType.STUDENT)
        .status(UserStatus.ACTIVE)
        .build();

    when(userService.findOrCreateStudent(eq("B22DCCN356"), eq("Phan Thị Hồng Huế"), eq("D22CN08"), eq(null)))
        .thenReturn(mockUser);

    when(enrollmentRepository.existsByClassSectionIdAndStudentId(sectionId, mockUser.getId()))
        .thenReturn(false);

    List<ImportRowError> errors = new ArrayList<>();
    TimetableProcessResult result = studentEnrollmentService.processStudentEnrollments(
        sectionId, List.of(studentDto), errors
    );

    assertEquals(1, result.getSuccessCount());
    assertTrue(errors.isEmpty());
    verify(enrollmentRepository).save(any(ClassEnrollment.class));
  }

  @Test
  @DisplayName("processStudentEnrollments: Đã ghi danh từ trước thì không tạo trùng")
  void testAlreadyEnrolled() {
    Long sectionId = 1L;
    ClassSection section = ClassSection.builder()
        .id(sectionId)
        .classCode("INT1306_01")
        .semesterId(10L)
        .build();

    when(classSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

    StudentEnrollmentRowDto studentDto = StudentEnrollmentRowDto.builder()
        .studentCode("B22DCCN356")
        .fullName("Phan Thị Hồng Huế")
        .managementClass("D22CN08")
        .build();

    User mockUser = User.builder()
        .id(32L)
        .code("B22DCCN356")
        .build();

    when(userService.findOrCreateStudent(eq("B22DCCN356"), eq("Phan Thị Hồng Huế"), eq("D22CN08"), eq(null)))
        .thenReturn(mockUser);

    when(enrollmentRepository.existsByClassSectionIdAndStudentId(sectionId, mockUser.getId()))
        .thenReturn(true);

    List<ImportRowError> errors = new ArrayList<>();
    TimetableProcessResult result = studentEnrollmentService.processStudentEnrollments(
        sectionId, List.of(studentDto), errors
    );

    assertEquals(1, result.getSuccessCount());
    assertTrue(errors.isEmpty());
    verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
  }
}
