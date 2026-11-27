package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleAudienceResolver implements AudienceResolver {

  private static final Set<AccountType> ALLOWED_TARGET_ROLES = Set.of(
      AccountType.STUDENT,
      AccountType.LECTURER
  );

  private final UserService userService;

  @Override
  public TargetScopeType supportedScope() {
    return TargetScopeType.ROLE;
  }

  @Override
  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    AccountType targetRole = request.targetRole();
    if (targetRole == null || !ALLOWED_TARGET_ROLES.contains(targetRole)) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_ROLE_INVALID);
    }
    List<Long> userIds = userService.findActiveUserIdsByAccountTypes(List.of(targetRole));
    return new ResolvedAudience(new LinkedHashSet<>(userIds), targetRole.name());
  }
}
