package org.ptit.meeting.modules.auth.service.impl;

import static org.ptit.meeting.modules.auth.constant.enums.AccountType.LECTURER;

import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.constant.enums.RoleCode;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminCreateRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminPasswordResetRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminUpdateRequest;
import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;
import org.ptit.meeting.modules.auth.entity.Role;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.entity.UserRole;
import org.ptit.meeting.modules.auth.mapper.LecturerAdminMapper;
import org.ptit.meeting.modules.auth.repository.LecturerAdminRepository;
import org.ptit.meeting.modules.auth.repository.RoleRepository;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.repository.UserRoleRepository;
import org.ptit.meeting.modules.auth.service.LecturerAdminService;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LecturerAdminServiceImpl implements LecturerAdminService {

  private static final Set<String> SORT_FIELDS = Set.of(
      "id", "code", "fullName", "email", "department", "status", "createdAt", "updatedAt");

  private final LecturerAdminRepository lecturerRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final LecturerAdminMapper mapper;
  private final PasswordEncoder passwordEncoder;

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
    User lecturer = requireLecturer(id);
    return mapper.toResponse(lecturer);
  }

  @Override
  @Transactional
  public LecturerAdminResponse create(LecturerAdminCreateRequest request) {
    String code = clean(request.code());
    String fullName = clean(request.fullName());
    String department = clean(request.department());
    if (department == null && code != null) {
      department = PtitAccountUtils.resolveLecturerDepartment(code);
    }

    String email = clean(request.email());
    if (email == null) {
      email = PtitAccountUtils.generateStaffEmail(fullName);
      int counter = 1;
      while (userRepository.existsByEmail(email)) {
        String[] parts = email.split("@");
        email = parts[0] + counter + "@" + parts[1];
        counter++;
      }
    } else {
      if (userRepository.existsByEmail(email)) {
        throw new BusinessException(StatusCode.EXISTED, AuthErrorConstants.LECTURER_EMAIL_EXISTS);
      }
    }

    if (code != null && userRepository.existsByCode(code)) {
      throw new BusinessException(StatusCode.EXISTED, AuthErrorConstants.LECTURER_CODE_EXISTS);
    }

    String rawPassword = clean(request.temporaryPassword());
    if (rawPassword == null) {
      rawPassword = PtitAccountUtils.DEFAULT_PASSWORD;
    }

    User user = User.builder()
        .code(code)
        .fullName(fullName)
        .email(email)
        .department(department)
        .accountType(LECTURER)
        .status(UserStatus.ACTIVE)
        .mustChangePassword(true)
        .passwordHash(passwordEncoder.encode(rawPassword))
        .build();

    user = userRepository.save(user);

    Role lecturerRole = roleRepository.findByCode(RoleCode.ROLE_LECTURER.name())
        .orElseThrow(() -> new BusinessException(StatusCode.NOT_EXISTED, AuthErrorConstants.LECTURER_ROLE_MISSING));

    if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), lecturerRole.getId())) {
      userRoleRepository.save(UserRole.builder()
          .userId(user.getId())
          .roleId(lecturerRole.getId())
          .build());
    }

    log.info("(create) created lecturer id: {}, code: {}, email: {}", user.getId(), user.getCode(), user.getEmail());
    return mapper.toResponse(user);
  }

  @Override
  @Transactional
  public LecturerAdminResponse update(Long id, LecturerAdminUpdateRequest request) {
    User lecturer = requireLecturer(id);
    String code = clean(request.code());
    String fullName = clean(request.fullName());
    String email = clean(request.email());
    String department = clean(request.department());

    if (code != null && !code.equalsIgnoreCase(lecturer.getCode())) {
      Optional<User> byCode = userRepository.findByCode(code);
      if (byCode.isPresent() && !byCode.get().getId().equals(lecturer.getId())) {
        throw new BusinessException(StatusCode.EXISTED, AuthErrorConstants.LECTURER_CODE_EXISTS);
      }
      lecturer.setCode(code);
    }

    if (email != null && !email.equalsIgnoreCase(lecturer.getEmail())) {
      Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
      if (byEmail.isPresent() && !byEmail.get().getId().equals(lecturer.getId())) {
        throw new BusinessException(StatusCode.EXISTED, AuthErrorConstants.LECTURER_EMAIL_EXISTS);
      }
      lecturer.setEmail(email);
    }

    if (fullName != null) {
      lecturer.setFullName(fullName);
    }

    if (department != null) {
      lecturer.setDepartment(department);
    }

    lecturer = userRepository.save(lecturer);
    log.info("(update) updated lecturer id: {}, code: {}", lecturer.getId(), lecturer.getCode());
    return mapper.toResponse(lecturer);
  }

  @Override
  @Transactional
  public LecturerAdminResponse changeStatus(Long id, UserStatus status) {
    User lecturer = requireLecturer(id);
    lecturer.setStatus(status);
    lecturer = userRepository.save(lecturer);
    log.info("(changeStatus) updated lecturer id: {} to status: {}", lecturer.getId(), status);
    return mapper.toResponse(lecturer);
  }

  @Override
  @Transactional
  public void resetPassword(Long id, LecturerAdminPasswordResetRequest request) {
    User lecturer = requireLecturer(id);
    String rawPassword = clean(request != null ? request.temporaryPassword() : null);
    if (rawPassword == null) {
      rawPassword = PtitAccountUtils.DEFAULT_PASSWORD;
    }
    lecturer.setPasswordHash(passwordEncoder.encode(rawPassword));
    lecturer.setMustChangePassword(true);
    userRepository.save(lecturer);
    log.info("(resetPassword) reset password for lecturer id: {}", lecturer.getId());
  }

  @Override
  @Transactional
  public void delete(Long id) {
    User lecturer = requireLecturer(id);
    // Soft delete / inactive account to preserve integrity with timetable
    lecturer.setStatus(UserStatus.INACTIVE);
    userRepository.save(lecturer);
    log.info("(delete) set status INACTIVE for lecturer id: {}", id);
  }

  private User requireLecturer(Long id) {
    return lecturerRepository.findByIdAndAccountType(id, LECTURER)
        .orElseThrow(() -> new BusinessException(StatusCode.NOT_EXISTED, AuthErrorConstants.LECTURER_NOT_FOUND));
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
