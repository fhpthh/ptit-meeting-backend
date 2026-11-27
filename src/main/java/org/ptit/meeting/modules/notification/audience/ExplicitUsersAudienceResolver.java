package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExplicitUsersAudienceResolver implements AudienceResolver {

  private final UserService userService;

  @Override
  public TargetScopeType supportedScope() {
    return TargetScopeType.USERS;
  }

  @Override
  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    List<Long> requestedIds = request.userIds();
    if (requestedIds == null || requestedIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_USERS_REQUIRED);
    }
    Set<Long> normalizedIds = requestedIds.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (normalizedIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_USERS_REQUIRED);
    }

    List<Long> activeUserIds = userService.findActiveUserIdsByIds(normalizedIds);
    String targetRef = String.valueOf(activeUserIds.size());
    return new ResolvedAudience(new LinkedHashSet<>(activeUserIds), targetRef);
  }
}
