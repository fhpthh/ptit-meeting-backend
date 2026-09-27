package org.ptit.meeting.dto.response;

import java.util.Set;
import lombok.Builder;
import org.ptit.meeting.constant.enums.AccountType;

@Builder
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean mustChangePassword,
    UserSummary user
) {

  @Builder
  public record UserSummary(
      Long id,
      String email,
      String code,
      String fullName,
      AccountType accountType,
      String department,
      Set<String> roles,
      Set<String> permissions
  ) {}
}
