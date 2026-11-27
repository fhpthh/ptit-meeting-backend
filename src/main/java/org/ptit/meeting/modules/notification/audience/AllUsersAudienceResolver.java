package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllUsersAudienceResolver implements AudienceResolver {

  private static final List<AccountType> BROADCAST_ACCOUNT_TYPES = List.of(
      AccountType.STUDENT,
      AccountType.LECTURER
  );

  private final UserService userService;

  @Override
  public TargetScopeType supportedScope() {
    return TargetScopeType.ALL;
  }

  @Override
  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    List<Long> userIds = userService.findActiveUserIdsByAccountTypes(BROADCAST_ACCOUNT_TYPES);
    return new ResolvedAudience(new LinkedHashSet<>(userIds), TargetScopeType.ALL.name());
  }
}
