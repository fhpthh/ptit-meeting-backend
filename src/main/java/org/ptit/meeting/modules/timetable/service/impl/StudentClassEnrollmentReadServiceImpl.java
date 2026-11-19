package org.ptit.meeting.modules.timetable.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.service.StudentClassEnrollmentReadService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentClassEnrollmentReadServiceImpl implements StudentClassEnrollmentReadService {
  private final ClassEnrollmentRepository enrollmentRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<StudentClassEnrollmentResponse> findClassEnrollments(
      Long studentId, Long semesterId, int page, int size) {
    return PageResponseDTO.from(enrollmentRepository.findStudentClassEnrollments(
        studentId, semesterId, PageRequest.of(page, size)));
  }
}
