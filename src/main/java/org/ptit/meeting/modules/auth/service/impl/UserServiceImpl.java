package org.ptit.meeting.modules.auth.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.ErrorConstants;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.RoleCode;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.entity.UserRole;
import org.ptit.meeting.modules.auth.repository.RoleRepository;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.repository.UserRoleRepository;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional(readOnly = true)
  public User authenticateWithPassword(String username, String rawPassword) {
    log.info("(authenticateWithPassword) username: {}", username);

    User user = userRepository.findByEmailOrCode(username, username)
        .orElseThrow(() -> new BusinessException(
            StatusCode.AUTHENTICATION_FAILED,
            AuthErrorConstants.INVALID_CREDENTIALS
        ));

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      log.warn("(authenticateWithPassword) Password mismatch for username: {}", username);
      throw new BusinessException(
          StatusCode.AUTHENTICATION_FAILED,
          AuthErrorConstants.INVALID_CREDENTIALS
      );
    }

    enrichAuthorities(user);
    return user;
  }

  @Override
  public void validateUserStatus(User user) {
    if (!user.isActive()) {
      log.warn("(validateUserStatus) User is not active: userId={}, status={}", user.getId(),
          user.getStatus());
      throw new BusinessException(StatusCode.ACCOUNT_LOCKED, AuthErrorConstants.ACCOUNT_LOCKED);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserById(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> BusinessException.notExisted(ErrorConstants.DATA_NOT_FOUND, userId));
    enrichAuthorities(user);
    return user;
  }

  @Override
  @Transactional
  public User findOrCreateByEmail(String email, String fullName, String code,
      AccountType accountType) {
    log.info("(findOrCreateByEmail) email: {}, code: {}, accountType: {}", email, code,
        accountType);
    String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
    String normalizedCode = PtitAccountUtils.normalizeToFullStudentCode(code);
    // Tầng 1: Tìm ưu tiên theo email chính thức
    Optional<User> userOpt = Optional.empty();
    if (normalizedEmail != null) {
      userOpt = userRepository.findByEmail(normalizedEmail);
    }
    // Tầng 2: Tìm theo mã sinh viên chuẩn có "DC" (VD: B22DCCN356)
    if (userOpt.isEmpty() && normalizedCode != null && !normalizedCode.isBlank()) {
      userOpt = userRepository.findByCode(normalizedCode);
    }
    // Tầng 3: Tìm theo mã rút gọn không có "DC" (VD: B22CN356)
    if (userOpt.isEmpty() && normalizedCode != null && !normalizedCode.isBlank()) {
      String shortCode = PtitAccountUtils.normalizeToShortStudentCode(normalizedCode);
      userOpt = userRepository.findByCode(shortCode);
    }
    return userOpt
        .map(existingUser -> {
          boolean modified = false;
          // Đồng bộ email chuẩn từ Outlook vào nếu trước đó lưu email tạm
          if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(
              existingUser.getEmail())) {
            log.info("(findOrCreateByEmail) Updating user email with Outlook email: '{}' -> '{}'",
                existingUser.getEmail(), normalizedEmail);
            existingUser.setEmail(normalizedEmail);
            modified = true;
          }
          if (normalizedCode != null && !normalizedCode.equalsIgnoreCase(existingUser.getCode())) {
            existingUser.setCode(normalizedCode);
            modified = true;
          }
          if (modified) {
            existingUser = userRepository.save(existingUser);
          }
          User synced = syncUserProfile(existingUser, fullName, normalizedCode);
          enrichAuthorities(synced);
          return synced;
        })
        .orElseGet(() -> {
          log.info("(findOrCreateByEmail) JIT provisioning new user: {}", email);
          User newUser = User.builder()
              .email(normalizedEmail)
              .fullName(fullName != null && !fullName.isBlank() ? fullName : email)
              .code(normalizedCode)
              .accountType(accountType)
              .status(UserStatus.ACTIVE)
              .mustChangePassword(false)
              .build();
          User saved = userRepository.save(newUser);
          RoleCode defaultRole = (accountType == AccountType.STUDENT)
              ? RoleCode.ROLE_STUDENT
              : RoleCode.ROLE_LECTURER;
          roleRepository.findByCode(defaultRole.name()).ifPresent(role -> {
            userRoleRepository.save(UserRole.builder()
                .userId(saved.getId())
                .roleId(role.getId())
                .build());
          });
          enrichAuthorities(saved);
          return saved;
        });
  }

  @Override
  @Transactional
  public User syncUserProfile(User user, String fullName, String code) {
    boolean modified = false;
    if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
      log.info("(syncUserProfile) Auto-updating fullName for user {}: '{}' -> '{}'",
          user.getEmail(), user.getFullName(), fullName);
      user.setFullName(fullName);
      modified = true;
    }
    if (code != null && !code.isBlank() && (user.getCode() == null || user.getCode().isBlank())) {
      log.info("(syncUserProfile) Auto-updating student code for user {}: '{}'", user.getEmail(),
          code);
      user.setCode(code);
      modified = true;
    }
    if (modified) {
      User saved = userRepository.save(user);
      enrichAuthorities(saved);
      return saved;
    }
    return user;
  }

  @Override
  @Transactional
  public User findOrCreateStudent(String studentCode, String fullName, String department,
      String email) {
    String normalizedCode = PtitAccountUtils.normalizeToFullStudentCode(studentCode);
    if (normalizedCode == null || normalizedCode.isBlank()) {
      throw BusinessException.invalidInput("validation.student.code.required");
    }
    // Tự động sinh email chuẩn Office 365 PTIT nếu chưa có
    String targetEmail = (email != null && !email.isBlank())
        ? email.trim().toLowerCase()
        : PtitAccountUtils.generateStudentEmail(fullName, normalizedCode);
    // 1. Kiểm tra tồn tại theo mã sinh viên (cả bản đầy đủ lẫn bản rút gọn)
    Optional<User> userOpt = userRepository.findByCode(normalizedCode);
    if (userOpt.isEmpty()) {
      String shortCode = PtitAccountUtils.normalizeToShortStudentCode(normalizedCode);
      userOpt = userRepository.findByCode(shortCode);
    }
    // 2. Kiểm tra tồn tại theo email
    if (userOpt.isEmpty() && targetEmail != null) {
      userOpt = userRepository.findByEmail(targetEmail);
    }
    // 3. Nếu sinh viên đã tồn tại, đồng bộ thông tin nếu cần
    if (userOpt.isPresent()) {
      User existing = userOpt.get();
      boolean modified = false;
      if (existing.getCode() == null || !existing.getCode().equalsIgnoreCase(normalizedCode)) {
        existing.setCode(normalizedCode);
        modified = true;
      }
      if (fullName != null && !fullName.isBlank() && !fullName.equals(existing.getFullName())) {
        existing.setFullName(fullName.trim());
        modified = true;
      }
      if (department != null && !department.isBlank() && existing.getDepartment() == null) {
        existing.setDepartment(department.trim());
        modified = true;
      }
      if (modified) {
        existing = userRepository.save(existing);
      }
      enrichAuthorities(existing);
      return existing;
    }
    // 4. Khởi tạo tài khoản mới với mật khẩu mặc định Ptit@123 được mã hóa BCrypt
    User newStudent = User.builder()
        .code(normalizedCode)
        .fullName(fullName != null && !fullName.isBlank() ? fullName.trim() : normalizedCode)
        .email(targetEmail)
        .department(department != null ? department.trim() : null)
        .passwordHash(passwordEncoder.encode(PtitAccountUtils.DEFAULT_STUDENT_PASSWORD))
        .accountType(AccountType.STUDENT)
        .status(UserStatus.ACTIVE)
        .mustChangePassword(true) // Yêu cầu đổi mật khẩu ở lần đăng nhập thường đầu tiên
        .build();
    User saved = userRepository.save(newStudent);
    // Gán vai trò ROLE_STUDENT
    roleRepository.findByCode(RoleCode.ROLE_STUDENT.name()).ifPresent(role -> {
      userRoleRepository.save(UserRole.builder()
          .userId(saved.getId())
          .roleId(role.getId())
          .build());
    });
    enrichAuthorities(saved);
    return saved;
  }

  @Override
  @Transactional
  public User changePassword(Long userId, String oldPassword, String newPassword) {
    log.info("(changePassword) userId: {}", userId);

    User user = getUserById(userId);

    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      log.warn("(changePassword) Old password mismatch for userId: {}", userId);
      throw new BusinessException(
          StatusCode.AUTHENTICATION_FAILED,
          AuthErrorConstants.INVALID_CREDENTIALS
      );
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setMustChangePassword(false);
    User saved = userRepository.save(user);
    enrichAuthorities(saved);
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public void enrichAuthorities(User user) {
    user.setRoles(roleRepository.findRoleCodesByUserId(user.getId()));
    user.setPermissions(roleRepository.findPermissionCodesByUserId(user.getId()));
  }
}
