package org.ptit.meeting.modules.notification.dto.internal;

import java.util.List;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;

public record AudienceResolutionRequest(
    TargetScopeType targetScope,
    AccountType targetRole,
    String targetDepartment,
    List<Long> classSectionIds,
    List<Long> userIds
) {
}
