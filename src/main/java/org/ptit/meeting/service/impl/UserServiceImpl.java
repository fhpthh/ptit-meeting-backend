package org.ptit.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.constant.enums.AccountType;
import org.ptit.meeting.constant.enums.RoleCode;
import org.ptit.meeting.constant.enums.UserStatus;
import org.ptit.meeting.entity.User;
import org.ptit.meeting.entity.UserRole;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.exception.ErrorCode;
import org.ptit.meeting.repository.RoleRepository;
import org.ptit.meeting.repository.UserRepository;
import org.ptit.meeting.repository.UserRoleRepository;
import org.ptit.meeting.service.UserService;
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
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      log.warn("(authenticateWithPassword) Password mismatch for username: {}", username);
      throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    enrichAuthorities(user);
    return user;
  }

  @Override
  public void validateUserStatus(User user) {
    if (!user.isActive()) {
      log.warn("(validateUserStatus) User is not active: userId={}, status={}", user.getId(), user.getStatus());
      throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserById(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    enrichAuthorities(user);
    return user;
  }

  @Override
  @Transactional
  public User findOrCreateByEmail(String email, String fullName, String code, AccountType accountType) {
    log.info("(findOrCreateByEmail) email: {}, accountType: {}", email, accountType);

    return userRepository.findByEmail(email)
        .map(existingUser -> {
          User synced = syncUserProfile(existingUser, fullName, code);
          enrichAuthorities(synced);
          return synced;
        })
        .orElseGet(() -> {
          log.info("(findOrCreateByEmail) JIT provisioning new user: {}", email);
          User newUser = User.builder()
              .email(email)
              .fullName(fullName != null && !fullName.isBlank() ? fullName : email)
              .code(code)
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
      log.info("(syncUserProfile) Auto-updating student code for user {}: '{}'", user.getEmail(), code);
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
  public User changePassword(Long userId, String oldPassword, String newPassword) {
    log.info("(changePassword) userId: {}", userId);

    User user = getUserById(userId);

    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      log.warn("(changePassword) Old password mismatch for userId: {}", userId);
      throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
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
