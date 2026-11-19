package org.ptit.meeting.modules.auth.service.impl;

import static org.ptit.meeting.modules.auth.constant.enums.AccountType.STUDENT;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.constant.enums.RoleCode;
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
import org.ptit.meeting.modules.auth.entity.Role;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.entity.UserRole;
import org.ptit.meeting.modules.auth.exporter.StudentAdminExporter;
import org.ptit.meeting.modules.auth.mapper.StudentAdminMapper;
import org.ptit.meeting.modules.auth.repository.RoleRepository;
import org.ptit.meeting.modules.auth.repository.StudentAdminRepository;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.repository.UserRoleRepository;
import org.ptit.meeting.modules.auth.service.StudentAdminService;
import org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse;
import org.ptit.meeting.modules.timetable.service.StudentClassEnrollmentReadService;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;
import org.ptit.meeting.modules.student.service.StudentAcademicProfileService;
import org.ptit.meeting.modules.student.service.StudentStructureService;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAdminServiceImpl implements StudentAdminService {
  private static final int EXPORT_LIMIT = 10_000;
  private static final Set<String> SORT_FIELDS = Set.of(
      "id", "code", "fullName", "email", "department", "status", "createdAt", "updatedAt");

  private final StudentAdminRepository students;
  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final PasswordEncoder passwordEncoder;
  private final StudentAdminMapper mapper;
  private final StudentAdminExporter exporter;
  private final StudentClassEnrollmentReadService enrollmentReadService;
  private final StudentAcademicProfileService academicProfiles;
  private final StudentStructureService studentStructure;

  @Override
  public PageResponseDTO<StudentAdminResponse> search(StudentAdminSearchRequest request) {
    Page<User> page = find(request, PageRequest.of(request.getPage(), request.getSize(), sort(request)));
    Map<Long, StudentAcademicProfileResponse> profileByUserId = academicProfiles.findByUserIds(
        page.getContent().stream().map(User::getId).toList());
    return PageResponseDTO.from(page.map(user -> mapper.toResponse(user, profileByUserId.get(user.getId()))));
  }

  @Override
  public StudentAdminResponse get(Long id) {
    User student = requireStudent(id);
    return mapper.toResponse(student, academicProfiles.findByUserId(student.getId()));
  }

  @Override
  @Transactional
  public StudentAdminResponse create(StudentAdminCreateRequest request) {
    String hash = encodeTemporaryPassword(request.temporaryPassword());
    Role role = lockStudentRole();
    User student = User.builder().accountType(STUDENT).passwordHash(hash).build();
    applyProfile(student, request.email(), request.code(), request.fullName(), request.department());
    students.saveAndFlush(student);
    userRoles.save(UserRole.builder().userId(student.getId()).roleId(role.getId()).build());
    StudentAcademicProfileResponse profile = request.academicProfile() == null ? null
        : academicProfiles.upsert(student.getId(), request.academicProfile());
    return mapper.toResponse(student, profile);
  }

  @Override
  @Transactional
  public StudentAdminResponse update(Long id, StudentAdminUpdateRequest request) {
    lockStudentRole();
    User student = lockStudent(id);
    applyProfile(student, request.email(), request.code(), request.fullName(), request.department());
    students.saveAndFlush(student);
    StudentAcademicProfileResponse profile = request.academicProfile() == null
        ? academicProfiles.findByUserId(student.getId())
        : academicProfiles.upsert(student.getId(), request.academicProfile());
    return mapper.toResponse(student, profile);
  }

  @Override
  @Transactional
  public StudentAcademicProfileResponse assignAcademicProfile(
      Long id, StudentAcademicAssignmentRequest request) {
    requireStudent(id);
    return academicProfiles.upsert(id, request);
  }

  @Override
  @Transactional
  public StudentAdminResponse changeStatus(Long id, UserStatus status) {
    User student = lockStudent(id);
    student.setStatus(status);
    students.flush();
    return mapper.toResponse(student, academicProfiles.findByUserId(student.getId()));
  }

  @Override
  @Transactional
  public StudentAdminBulkStatusResponse changeStatuses(StudentAdminBulkStatusRequest request) {
    Set<Long> ids = new HashSet<>(request.ids());
    List<User> accounts = students.lockAccounts(STUDENT, ids);
    if (accounts.size() != ids.size()) {
      Set<Long> found = new HashSet<>();
      accounts.forEach(account -> found.add(account.getId()));
      throw notFound(ids.stream().filter(id -> !found.contains(id)).min(Long::compareTo).orElseThrow());
    }
    int changed = 0;
    for (User account : accounts) {
      if (account.getStatus() != request.status()) {
        account.setStatus(request.status());
        changed++;
      }
    }
    return new StudentAdminBulkStatusResponse(accounts.size(), changed);
  }

  @Override
  @Transactional
  public void resetPassword(Long id, StudentAdminPasswordResetRequest request) {
    String hash = encodeTemporaryPassword(request.temporaryPassword());
    User student = lockStudent(id);
    student.setPasswordHash(hash);
    student.setMustChangePassword(true);
  }

  @Override
  @Transactional
  public void archive(Long id) {
    lockStudent(id).setStatus(UserStatus.INACTIVE);
  }

  @Override
  public StudentAdminSummaryResponse summary() {
    long active = students.countByAccountTypeAndStatus(STUDENT, UserStatus.ACTIVE);
    long locked = students.countByAccountTypeAndStatus(STUDENT, UserStatus.LOCKED);
    long inactive = students.countByAccountTypeAndStatus(STUDENT, UserStatus.INACTIVE);
    return new StudentAdminSummaryResponse(active + locked + inactive, active, locked, inactive);
  }

  @Override
  public StudentAdminFiltersResponse filters() {
    return new StudentAdminFiltersResponse(List.of(UserStatus.values()), students.findDepartments(STUDENT),
        studentStructure.options(null, null, null));
  }

  @Override
  public StudentAdminExportFile export(StudentAdminSearchRequest request, StudentAdminExportFormat format) {
    Page<User> results = find(request, PageRequest.of(0, EXPORT_LIMIT + 1, sort(request)));
    if (results.getTotalElements() > EXPORT_LIMIT) {
      throw BusinessException.invalidInput(AuthErrorConstants.STUDENT_EXPORT_LIMIT, EXPORT_LIMIT);
    }
    return exporter.export(results.map(mapper::toResponse).getContent(), format);
  }

  @Override
  public PageResponseDTO<StudentClassEnrollmentResponse> enrollments(
      Long id, Long semesterId, int page, int size) {
    requireStudent(id);
    return enrollmentReadService.findClassEnrollments(id, semesterId, page, size);
  }

  private Page<User> find(StudentAdminSearchRequest request, Pageable pageable) {
    String keyword = trimToNull(request.getKeyword());
    String pattern = keyword == null ? null : "%" + keyword.toLowerCase(Locale.ROOT)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    String department = trimToNull(request.getDepartment());
    return students.search(STUDENT, request.getStatus(),
        department == null ? null : department.toLowerCase(Locale.ROOT),
        request.getCampusId(), request.getCohortId(), request.getMajorId(),
        request.getAdministrativeClassId(), request.getStudyStatus(), pattern, pageable);
  }

  private Sort sort(StudentAdminSearchRequest request) {
    String field = request.getSortBy();
    if (field == null || !SORT_FIELDS.contains(field) || request.getDirection() == null) {
      throw BusinessException.invalidInput(AuthErrorConstants.STUDENT_SORT_INVALID);
    }
    Sort sort = Sort.by(request.getDirection(), field);
    return "id".equals(field) ? sort : sort.and(Sort.by("id"));
  }

  private Role lockStudentRole() {
    // Serialize admin identity writes because legacy users.code has a non-unique index.
    return roles.lockByCode(RoleCode.ROLE_STUDENT.name())
        .orElseThrow(() -> new BusinessException(StatusCode.ERROR_IN_BACKEND,
            AuthErrorConstants.STUDENT_ROLE_MISSING));
  }

  private User requireStudent(Long id) {
    return students.findByIdAndAccountType(id, STUDENT).orElseThrow(() -> notFound(id));
  }

  private User lockStudent(Long id) {
    return students.lockAccounts(STUDENT, List.of(id)).stream().findFirst()
        .orElseThrow(() -> notFound(id));
  }

  private BusinessException notFound(Long id) {
    return BusinessException.notExisted(AuthErrorConstants.STUDENT_NOT_FOUND, id);
  }

  private void applyProfile(User student, String email, String code, String fullName, String department) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
    String normalizedCode = PtitAccountUtils.normalizeToFullStudentCode(code.toUpperCase(Locale.ROOT));
    if (normalizedCode.length() > 30) {
      throw BusinessException.invalidInput(AuthErrorConstants.STUDENT_CODE_INVALID);
    }
    if (!normalizedEmail.equalsIgnoreCase(student.getEmail())
        && users.findByEmailIgnoreCase(normalizedEmail).filter(user -> isOtherUser(student, user)).isPresent()) {
      throw BusinessException.existed(AuthErrorConstants.STUDENT_EMAIL_EXISTS);
    }
    if (!normalizedCode.equalsIgnoreCase(student.getCode())) {
      String shortCode = PtitAccountUtils.normalizeToShortStudentCode(normalizedCode);
      boolean codeExists = codeUsedByAnother(student, normalizedCode)
          || (!normalizedCode.equals(shortCode) && codeUsedByAnother(student, shortCode));
      if (codeExists) {
        throw BusinessException.existed(AuthErrorConstants.STUDENT_CODE_EXISTS);
      }
    }
    student.setEmail(normalizedEmail);
    student.setCode(normalizedCode);
    student.setFullName(fullName.strip());
    student.setDepartment(trimToNull(department));
  }

  private boolean codeUsedByAnother(User student, String code) {
    return users.findAllByCodeIgnoreCase(code).stream().anyMatch(user -> isOtherUser(student, user));
  }

  private boolean isOtherUser(User student, User candidate) {
    return !Objects.equals(student.getId(), candidate.getId());
  }

  private String encodeTemporaryPassword(String password) {
    if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.indexOf(0) >= 0) {
      throw BusinessException.invalidInput(AuthErrorConstants.STUDENT_PASSWORD_SIZE);
    }
    return passwordEncoder.encode(password);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
