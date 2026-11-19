package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.auth.constant.enums.StudentAdminExportFormat;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.dto.internal.StudentAdminExportFile;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminBulkStatusRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminCreateRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminPasswordResetRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminUpdateRequest;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminBulkStatusResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminFiltersResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminSummaryResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;

public interface StudentAdminService {
  PageResponseDTO<StudentAdminResponse> search(StudentAdminSearchRequest request);
  StudentAdminResponse get(Long id);
  StudentAdminResponse create(StudentAdminCreateRequest request);
  StudentAdminResponse update(Long id, StudentAdminUpdateRequest request);
  StudentAcademicProfileResponse assignAcademicProfile(
      Long id, StudentAcademicAssignmentRequest request);
  StudentAdminResponse changeStatus(Long id, UserStatus status);
  StudentAdminBulkStatusResponse changeStatuses(StudentAdminBulkStatusRequest request);
  void resetPassword(Long id, StudentAdminPasswordResetRequest request);
  void archive(Long id);
  StudentAdminSummaryResponse summary();
  StudentAdminFiltersResponse filters();
  StudentAdminExportFile export(StudentAdminSearchRequest request, StudentAdminExportFormat format);
  PageResponseDTO<StudentClassEnrollmentResponse> enrollments(Long id, Long semesterId, int page, int size);
}
