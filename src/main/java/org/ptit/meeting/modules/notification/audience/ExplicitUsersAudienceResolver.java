package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    Set<String> normalizedEmails = request.userEmails() == null
        ? Set.of()
        : request.userEmails().stream()
            .filter(email -> email != null && !email.isBlank())
            .map(email -> email.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    Set<Long> normalizedIds = request.userIds() == null
        ? Set.of()
        : request.userIds().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    if (normalizedEmails.isEmpty() && normalizedIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_USERS_REQUIRED);
    }

    Set<Long> resolvedUserIds = new LinkedHashSet<>();

    // Map recipient emails to active user IDs and fail fast if any email cannot be found.
    if (!normalizedEmails.isEmpty()) {
      Map<String, Long> emailToUserId = userService.findActiveUserIdsByEmails(normalizedEmails);
      List<String> missingEmails = normalizedEmails.stream()
          .filter(email -> !emailToUserId.containsKey(email))
          .toList();
      if (!missingEmails.isEmpty()) {
        throw BusinessException.invalidInput(
            NotificationErrorConstants.TARGET_USER_EMAILS_NOT_FOUND,
            String.join(", ", missingEmails)
        );
      }
      resolvedUserIds.addAll(emailToUserId.values());
    }

    if (!normalizedIds.isEmpty()) {
      resolvedUserIds.addAll(userService.findActiveUserIdsByIds(normalizedIds));
    }

    String targetRef = normalizedEmails.isEmpty()
        ? String.valueOf(resolvedUserIds.size())
        : String.join(", ", normalizedEmails);
    return new ResolvedAudience(resolvedUserIds, targetRef);
  }
}
