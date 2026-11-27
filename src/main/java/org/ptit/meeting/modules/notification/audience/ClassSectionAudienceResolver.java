package org.ptit.meeting.modules.notification.audience;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassSectionAudienceResolver implements AudienceResolver {

  private final ClassSectionAudienceService classSectionAudienceService;

  @Override
  public TargetScopeType supportedScope() {
    return TargetScopeType.CLASS_SECTION;
  }

  @Override
  public ResolvedAudience resolve(AudienceResolutionRequest request) {
    List<Long> sectionIds = request.classSectionIds();
    if (sectionIds == null || sectionIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_CLASS_SECTIONS_REQUIRED);
    }
    Set<Long> normalizedIds = sectionIds.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (normalizedIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_CLASS_SECTIONS_REQUIRED);
    }

    Set<Long> memberIds = classSectionAudienceService.findActiveMemberUserIds(normalizedIds);
    String targetRef = normalizedIds.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    return new ResolvedAudience(memberIds, targetRef);
  }
}
