package org.ptit.meeting.modules.auth.service.impl;

import static org.ptit.meeting.modules.auth.constant.enums.AccountType.LECTURER;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.mapper.LecturerAdminMapper;
import org.ptit.meeting.modules.auth.repository.LecturerAdminRepository;
import org.ptit.meeting.modules.auth.service.LecturerAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LecturerAdminServiceImpl implements LecturerAdminService {

  private static final Set<String> SORT_FIELDS = Set.of(
      "id", "code", "fullName", "email", "department", "status", "createdAt", "updatedAt");

  private final LecturerAdminRepository lecturerRepository;
  private final LecturerAdminMapper mapper;

  @Override
  public PageResponseDTO<LecturerAdminResponse> search(LecturerAdminSearchRequest request) {
    Page<User> page = lecturerRepository.search(
        LECTURER,
        request.getStatus(),
        clean(request.getDepartment()),
        clean(request.getKeyword()),
        PageRequest.of(request.getPage(), request.getSize(), sort(request))
    );
    return PageResponseDTO.from(page.map(mapper::toResponse));
  }

  @Override
  public LecturerAdminResponse get(Long id) {
    User lecturer = lecturerRepository.findByIdAndAccountType(id, LECTURER)
        .orElseThrow(() -> new BusinessException(StatusCode.NOT_EXISTED, AuthErrorConstants.LECTURER_NOT_FOUND));
    return mapper.toResponse(lecturer);
  }

  private String clean(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Sort sort(LecturerAdminSearchRequest request) {
    Sort.Direction direction = request.getDirection() == null ? Sort.Direction.ASC : request.getDirection();
    String field = request.getSortBy();
    if (field == null || !SORT_FIELDS.contains(field.trim())) {
      field = "fullName";
    }
    return Sort.by(direction, field.trim()).and(Sort.by(Sort.Direction.ASC, "id"));
  }
}
