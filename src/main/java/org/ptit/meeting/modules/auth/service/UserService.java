
package org.ptit.meeting.modules.auth.service;

import java.util.Collection;
import java.util.List;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.entity.User;

public interface UserService {

  User authenticateWithPassword(String username, String rawPassword);

  void validateUserStatus(User user);

  User getUserById(Long userId);

  List<User> getUsersByIds(Collection<Long> userIds);

  User findOrCreateByEmail(String email, String fullName, String code, AccountType accountType);

  User changePassword(Long userId, String oldPassword, String newPassword);

  void enrichAuthorities(User user);

  User syncUserProfile(User user, String fullName, String code);

  User findOrCreateStudent(String studentCode, String fullName, String department, String email);
}
