package org.ptit.meeting.modules.notification.dto.internal;

import java.util.Set;

public record ResolvedAudience(
    Set<Long> recipientUserIds,
    String targetRef
) {
}
