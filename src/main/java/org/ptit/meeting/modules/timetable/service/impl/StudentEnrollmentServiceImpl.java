package org.ptit.meeting.modules.timetable.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.dto.internal.StudentEnrollmentRowDto;
import org.ptit.meeting.modules.timetable.dto.internal.TimetableProcessResult;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.event.StudentEnrolledInClassSectionEvent;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.service.StudentEnrollmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEnrollmentServiceImpl implements StudentEnrollmentService {

  private final ClassSectionRepository classSectionRepository;
  private final ClassEnrollmentRepository enrollmentRepository;
  private final ClassSchedulerRepository classSchedulerRepository;
  private final UserService userService;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public TimetableProcessResult processStudentEnrollments(
      Long classSectionId,
      List<StudentEnrollmentRowDto> rows,
      List<ImportRowError> errors
  ) {
    // 1. Kiểm tra tồn tại lớp học phần
    ClassSection classSection = classSectionRepository.findById(classSectionId)
        .orElseThrow(() -> BusinessException.notExisted(TimetableErrorConstants.CLASS_SECTION_NOT_FOUND));

    int successCount = 0;
    Set<Long> newlyEnrolledStudentIds = new LinkedHashSet<>();

    for (int i = 0; i < rows.size(); i++) {
      StudentEnrollmentRowDto row = rows.get(i);
      int rowNumber = i + 2; // Dòng Excel thực tế sau Header

      try {
        // 2. Tạo hoặc lấy tài khoản sinh viên (mật khẩu Ptit@123, email Office 365, ROLE_STUDENT)
        User student = userService.findOrCreateStudent(
            row.getStudentCode(),
            row.getFullName(),
            row.getManagementClass(),
            null // Tự sinh email chuẩn HuePTH.B22CN356@stu.ptit.edu.vn
        );

        // 3. Ghi danh lớp học phần (idempotency - nếu chưa ghi danh thì thêm mới)
        boolean alreadyEnrolled = enrollmentRepository.existsByClassSectionIdAndStudentId(
            classSection.getId(), student.getId()
        );

        if (!alreadyEnrolled) {
          ClassEnrollment enrollment = ClassEnrollment.builder()
              .classSectionId(classSection.getId())
              .userId(student.getId())
              .status(EnrollmentStatus.ENROLLED)
              .build();
          enrollmentRepository.save(enrollment);
          newlyEnrolledStudentIds.add(student.getId());
        }

        successCount++;
      } catch (Exception ex) {
        log.error("(processStudentEnrollments) Error at row {}: {}", rowNumber, ex.getMessage());
        errors.add(new ImportRowError(rowNumber, "Lỗi sinh viên " + row.getStudentCode() + ": " + ex.getMessage()));
      }
    }

    if (eventPublisher != null
        && !newlyEnrolledStudentIds.isEmpty()
        && classSchedulerRepository != null
        && classSchedulerRepository.existsByClassSectionIdAndIsOnlineTrue(classSection.getId())) {
      eventPublisher.publishEvent(new StudentEnrolledInClassSectionEvent(
          classSection.getId(),
          classSection.getClassCode(),
          newlyEnrolledStudentIds
      ));
    }

    return TimetableProcessResult.builder()
        .successCount(successCount)
        .build();
  }
}
