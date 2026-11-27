package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
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
public class DepartmentAudienceResolver implements AudienceResolver {

  private final UserService userService;

  @Override
  public TargetScopeType supportedScope() {
    return TargetScopeType.DEPARTMENT;
  }

  @Override
  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    String department = request.targetDepartment();
    if (department == null || department.isBlank()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_DEPARTMENT_REQUIRED);
    }
    String normalized = department.trim();
    List<Long> userIds = userService.findActiveUserIdsByDepartment(normalized);
    return new ResolvedAudience(new LinkedHashSet<>(userIds), normalized);
  }
}
