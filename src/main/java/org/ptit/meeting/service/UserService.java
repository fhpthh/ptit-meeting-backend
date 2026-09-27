package org.ptit.meeting.service;

import org.ptit.meeting.constant.enums.AccountType;
import org.ptit.meeting.entity.User;

public interface UserService {

  User authenticateWithPassword(String username, String rawPassword);

  void validateUserStatus(User user);

  User getUserById(Long userId);

  User findOrCreateByEmail(String email, String fullName, String code, AccountType accountType);

  User changePassword(Long userId, String oldPassword, String newPassword);

  void enrichAuthorities(User user);

  User syncUserProfile(User user, String fullName, String code);
}
