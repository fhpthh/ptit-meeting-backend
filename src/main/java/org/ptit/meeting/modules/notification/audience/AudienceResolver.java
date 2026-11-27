package org.ptit.meeting.modules.notification.audience;

import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;

/**
 * Strategy contract for resolving notification recipient user IDs for a given target scope.
 */
public interface AudienceResolver {

  TargetScopeType supportedScope();

  ResolvedAudience resolve(AudienceResolutionRequest request);
}
