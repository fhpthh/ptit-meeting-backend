package org.ptit.meeting.modules.notification.audience;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.springframework.stereotype.Component;

@Component
public class AudienceResolverRegistry {

  private final Map<TargetScopeType, AudienceResolver> resolvers;

  public AudienceResolverRegistry(List<AudienceResolver> resolverList) {
    Map<TargetScopeType, AudienceResolver> map = new EnumMap<>(TargetScopeType.class);
    for (AudienceResolver resolver : resolverList) {
      map.put(resolver.supportedScope(), resolver);
    }
    this.resolvers = Map.copyOf(map);
  }

  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    if (request == null || request.targetScope() == null) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_SCOPE_INVALID);
    }
    AudienceResolver resolver = resolvers.get(request.targetScope());
    if (resolver == null) {
      throw BusinessException.invalidInput(
          NotificationErrorConstants.TARGET_SCOPE_INVALID,
          request.targetScope()
      );
    }
    return resolver.resolve(request);
  }
}
