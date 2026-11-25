package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;

public interface LecturerAdminService {
  PageResponseDTO<LecturerAdminResponse> search(LecturerAdminSearchRequest request);
  LecturerAdminResponse get(Long id);
}
