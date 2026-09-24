package org.ptit.meeting.layer.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.layer.domain.enums.AccountType;
import org.ptit.meeting.layer.domain.enums.UserStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  private Long id;
  private String email;
  private String passwordHAsh;
  private String fullName;
  private String code; // MSV_ B22DCCnXXX
  private AccountType accountType;
  private String department;
  private UserStatus status;
  private boolean mustChangePassword;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Builder.Default
  private Set<String> roles = new HashSet<>();
  @Builder.Default
  private Set<String> permissions = new HashSet<>();

  public boolean isActive() {
    return UserStatus.ACTIVE.equals(status);
  }

  public boolean isStudent() {
    return AccountType.STUDENT.equals(accountType);
  }
}
