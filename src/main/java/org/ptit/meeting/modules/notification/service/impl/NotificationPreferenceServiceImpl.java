package org.ptit.meeting.modules.notification.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.dto.request.NotificationPreferenceUpdateRequest;
import org.ptit.meeting.modules.notification.dto.response.NotificationPreferenceResponse;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationPreferenceRepository;
import org.ptit.meeting.modules.notification.service.NotificationPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

  private final NotificationPreferenceRepository preferenceRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationProperties properties;

  @Override
  @Transactional(readOnly = true)
  public NotificationPreferenceResponse getPreferences(Long userId) {
    NotificationPreference preference = preferenceRepository.findByUserId(userId)
        .orElseGet(() -> defaultPreference(userId));
    return notificationMapper.toPreferenceResponse(preference);
  }

  @Override
  @Transactional
  public NotificationPreferenceResponse updatePreferences(
      Long userId,
      NotificationPreferenceUpdateRequest request
  ) {
    int reminderMinutes = request.reminderMinutesBefore();
    if (reminderMinutes < properties.getMinReminderMinutes()
        || reminderMinutes > properties.getMaxReminderMinutes()) {
      throw BusinessException.invalidInput(
          NotificationErrorConstants.REMINDER_MINUTES_INVALID,
          properties.getMinReminderMinutes(),
          properties.getMaxReminderMinutes()
      );
    }

    NotificationPreference preference = preferenceRepository.findByUserId(userId)
        .orElseGet(() -> defaultPreference(userId));
    preference.setInAppEnabled(request.inAppEnabled());
    preference.setZaloEnabled(request.zaloEnabled());
    preference.setReminderMinutesBefore(reminderMinutes);
    preference.setReceivePgvViaZalo(request.receivePgvViaZalo());
    preference.setReceiveLecturerViaZalo(request.receiveLecturerViaZalo());

    NotificationPreference saved = preferenceRepository.save(preference);
    return notificationMapper.toPreferenceResponse(saved);
  }

  private NotificationPreference defaultPreference(Long userId) {
    return NotificationPreference.builder()
        .userId(userId)
        .inAppEnabled(true)
        .zaloEnabled(true)
        .reminderMinutesBefore(properties.getDefaultReminderMinutes())
        .receivePgvViaZalo(true)
        .receiveLecturerViaZalo(true)
        .build();
  }
}
