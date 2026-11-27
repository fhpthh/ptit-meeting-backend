package org.ptit.meeting.modules.notification.service;

import org.ptit.meeting.modules.notification.dto.request.NotificationPreferenceUpdateRequest;
import org.ptit.meeting.modules.notification.dto.response.NotificationPreferenceResponse;

public interface NotificationPreferenceService {

  NotificationPreferenceResponse getPreferences(Long userId);

  NotificationPreferenceResponse updatePreferences(
      Long userId,
      NotificationPreferenceUpdateRequest request
  );
}
