package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminCreateRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminPasswordResetRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminUpdateRequest;
import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;

public interface LecturerAdminService {
  PageResponseDTO<LecturerAdminResponse> search(LecturerAdminSearchRequest request);
  LecturerAdminResponse get(Long id);
  LecturerAdminResponse create(LecturerAdminCreateRequest request);
  LecturerAdminResponse update(Long id, LecturerAdminUpdateRequest request);
  LecturerAdminResponse changeStatus(Long id, UserStatus status);
  void resetPassword(Long id, LecturerAdminPasswordResetRequest request);
  void delete(Long id);
}
