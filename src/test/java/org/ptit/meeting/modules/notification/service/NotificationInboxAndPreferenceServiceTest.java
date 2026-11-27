package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.dto.request.NotificationPreferenceUpdateRequest;
import org.ptit.meeting.modules.notification.dto.response.NotificationPreferenceResponse;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationPreferenceRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.service.impl.NotificationInboxServiceImpl;
import org.ptit.meeting.modules.notification.service.impl.NotificationPreferenceServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationInboxAndPreferenceServiceTest {

  @Mock
  private NotificationRecipientRepository recipientRepository;
  @Mock
  private NotificationPreferenceRepository preferenceRepository;

  private NotificationInboxServiceImpl inboxService;
  private NotificationPreferenceServiceImpl preferenceService;

  @BeforeEach
  void setUp() {
    NotificationProperties properties = new NotificationProperties();
    NotificationMapper mapper = new NotificationMapper();
    inboxService = new NotificationInboxServiceImpl(recipientRepository, mapper, properties);
    preferenceService = new NotificationPreferenceServiceImpl(
        preferenceRepository, mapper, properties
    );
  }

  @Test
  void markAsRead_whenUnreadRecipientExists_marksReadAndSaves() {
    NotificationRecipient recipient = NotificationRecipient.builder()
        .notificationId("noti-1")
        .userId(10L)
        .read(false)
        .build();
    recipient.setId("rec-1");
    when(recipientRepository.findByIdAndUserId("rec-1", 10L))
        .thenReturn(Optional.of(recipient));

    inboxService.markAsRead(10L, "rec-1");

    assertTrue(recipient.isRead());
    assertNotNull(recipient.getReadAt());
    verify(recipientRepository).save(recipient);
  }

  @Test
  void markAsRead_whenRecipientMissing_throwsNotFound() {
    when(recipientRepository.findByIdAndUserId("missing", 10L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
        () -> inboxService.markAsRead(10L, "missing"));
    assertEquals(NotificationErrorConstants.RECIPIENT_NOT_FOUND, ex.getStrCode());
  }

  @Test
  void getPreferences_whenNotConfigured_returnsDefaultWith30Minutes() {
    when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.empty());

    NotificationPreferenceResponse response = preferenceService.getPreferences(10L);

    assertTrue(response.inAppEnabled());
    assertTrue(response.zaloEnabled());
    assertEquals(30, response.reminderMinutesBefore());
  }

  @Test
  void updatePreferences_whenReminderBelowMinimum30_throwsInvalidInput() {
    NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(
        true, true, 15, true, true
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> preferenceService.updatePreferences(10L, request));
    assertEquals(NotificationErrorConstants.REMINDER_MINUTES_INVALID, ex.getStrCode());
  }

  @Test
  void updatePreferences_whenValid_savesAndReturnsUpdated() {
    when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.empty());
    when(preferenceRepository.save(any(NotificationPreference.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(
        true, false, 45, false, true
    );

    NotificationPreferenceResponse response = preferenceService.updatePreferences(10L, request);

    assertEquals(45, response.reminderMinutesBefore());
    assertEquals(false, response.zaloEnabled());
    assertEquals(false, response.receivePgvViaZalo());
  }
}
