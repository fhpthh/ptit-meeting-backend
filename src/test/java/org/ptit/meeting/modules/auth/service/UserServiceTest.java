package org.ptit.meeting.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.RoleCode;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.Role;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.repository.RoleRepository;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.repository.UserRoleRepository;
import org.ptit.meeting.modules.auth.service.impl.UserServiceImpl;
import org.ptit.meeting.utils.PtitAccountUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private UserRoleRepository userRoleRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserServiceImpl userService;

  private Role studentRole;

  @BeforeEach
  void setUp() {
    studentRole = Role.builder().id(2L).code(RoleCode.ROLE_STUDENT.name()).build();
  }

  @Test
  @DisplayName("JIT Provisioning via Outlook assigns default password and student role")
  void testFindOrCreateByEmail_JitStudentProvisioning_HasDefaultPassword() {
    String email = "HuePTH.B22CN356@stu.ptit.edu.vn";
    String fullName = "Phan Thị Hồng Huế";
    String code = "B22DCCN356";

    when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());
    when(userRepository.findByCode(code)).thenReturn(Optional.empty());
    when(userRepository.findByCode("B22CN356")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(PtitAccountUtils.DEFAULT_STUDENT_PASSWORD)).thenReturn("$2a$10$encodedDefaultPassword");
    when(roleRepository.findByCode(RoleCode.ROLE_STUDENT.name())).thenReturn(Optional.of(studentRole));

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User u = invocation.getArgument(0);
      u.setId(100L);
      return u;
    });

    User created = userService.findOrCreateByEmail(email, fullName, code, AccountType.STUDENT);

    assertNotNull(created);
    assertEquals("B22DCCN356", created.getCode());
    assertEquals(email.toLowerCase(), created.getEmail());
    assertEquals("$2a$10$encodedDefaultPassword", created.getPasswordHash());
    assertTrue(created.isMustChangePassword());
    assertEquals(UserStatus.ACTIVE, created.getStatus());

    verify(userRoleRepository).save(any());
  }

  @Test
  @DisplayName("Login with password succeeds with full student code B22DCCN356 or school email")
  void testAuthenticateWithPassword_SuccessWithFullCodeAndEmail() {
    User existingUser = User.builder()
        .id(100L)
        .code("B22DCCN356")
        .email("huepth.b22cn356@stu.ptit.edu.vn")
        .passwordHash("$2a$10$encodedDefaultPassword")
        .status(UserStatus.ACTIVE)
        .build();

    when(userRepository.findByEmailOrCode("b22dccn356", "B22DCCN356")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("Ptit@123", "$2a$10$encodedDefaultPassword")).thenReturn(true);

    User authenticated = userService.authenticateWithPassword("B22DCCN356", "Ptit@123");

    assertNotNull(authenticated);
    assertEquals("B22DCCN356", authenticated.getCode());
  }

  @Test
  @DisplayName("Login with shortened student code B22CN356 is rejected with SHORT_CODE_NOT_ALLOWED")
  void testAuthenticateWithPassword_RejectsShortCode() {
    org.ptit.meeting.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
        org.ptit.meeting.exception.BusinessException.class,
        () -> userService.authenticateWithPassword("B22CN356", "Ptit@123")
    );

    assertEquals(org.ptit.meeting.modules.auth.constant.AuthErrorConstants.SHORT_CODE_NOT_ALLOWED, ex.getMessage());
  }
}
