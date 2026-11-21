package org.ptit.meeting.modules.timetable.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse;

public interface StudentClassEnrollmentReadService {
  PageResponseDTO<StudentClassEnrollmentResponse> findClassEnrollments(
      Long studentId, Long semesterId, int page, int size);
}
