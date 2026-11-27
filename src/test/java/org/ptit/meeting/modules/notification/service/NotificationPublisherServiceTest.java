package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.repository.NotificationDeliveryRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.impl.NotificationPublisherServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherServiceTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private NotificationRecipientRepository recipientRepository;
  @Mock
  private NotificationDeliveryRepository deliveryRepository;
  @Mock
  private NotificationDispatchService dispatchService;

  private NotificationProperties properties;
  private NotificationPublisherServiceImpl publisherService;

  @BeforeEach
  void setUp() {
    properties = new NotificationProperties();
    properties.setRecipientBatchSize(2);
    properties.setMaxTargetUsers(5);
    publisherService = new NotificationPublisherServiceImpl(
        notificationRepository,
        recipientRepository,
        deliveryRepository,
        dispatchService,
        properties
    );
  }

  @Test
  void publish_whenValidRecipients_persistsInBatchesAndReturnsCount() {
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
      Notification saved = inv.getArgument(0);
      saved.setId("noti-1");
      return saved;
    });

    Set<Long> recipients = new LinkedHashSet<>(java.util.List.of(1L, 2L, 3L));
    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.PGV_ANNOUNCEMENT,
        NotificationCategory.ACADEMIC,
        NotificationPriority.HIGH,
        "  Exam schedule  ",
        "  Midterm starts Monday  ",
        TargetScopeType.ROLE,
        "STUDENT",
        "PGV_ANNOUNCEMENT",
        null,
        null,
        10L,
        recipients,
        Set.of(DeliveryChannel.IN_APP)
    );

    NotificationPublishResult result = publisherService.publish(command);

    assertFalse(result.deduplicated());
    assertEquals(3L, result.totalRecipients());
    assertEquals("Exam schedule", result.notification().getTitle());
    verify(recipientRepository, times(2)).saveAll(anyList());
    verify(deliveryRepository, never()).saveAll(anyList());
    verify(dispatchService, never()).dispatchPendingDeliveries(any());
  }

  @Test
  void publish_whenExternalChannelRequested_persistsDeliveriesAndDispatches() {
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
      Notification saved = inv.getArgument(0);
      saved.setId("noti-2");
      return saved;
    });

    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.PGV_ANNOUNCEMENT,
        NotificationCategory.URGENT,
        NotificationPriority.URGENT,
        "Weather alert",
        "Classes moved online",
        TargetScopeType.USERS,
        "2",
        "PGV_ANNOUNCEMENT",
        null,
        null,
        10L,
        Set.of(101L, 102L),
        Set.of(DeliveryChannel.IN_APP, DeliveryChannel.ZALO)
    );

    publisherService.publish(command);

    verify(recipientRepository, times(1)).saveAll(anyList());
    verify(deliveryRepository, times(1)).saveAll(anyList());
    verify(dispatchService).dispatchPendingDeliveries("noti-2");
  }

  @Test
  void publish_whenDedupeKeyAlreadyExists_skipsRepublishing() {
    Notification existing = Notification.builder().title("Existing").build();
    existing.setId("existing-id");
    when(notificationRepository.findByDedupeKey("REMINDER:1:2026-11-27"))
        .thenReturn(Optional.of(existing));

    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.CLASS_REMINDER,
        NotificationCategory.TIMETABLE,
        NotificationPriority.NORMAL,
        "Reminder",
        "Class starts in 30m",
        TargetScopeType.CLASS_SECTION,
        "1",
        "CLASS_SCHEDULE",
        "1",
        "REMINDER:1:2026-11-27",
        null,
        Set.of(1L),
        Set.of(DeliveryChannel.IN_APP)
    );

    NotificationPublishResult result = publisherService.publish(command);

    assertTrue(result.deduplicated());
    assertEquals("existing-id", result.notification().getId());
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void publish_whenRecipientsEmpty_throwsInvalidInput() {
    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.PGV_ANNOUNCEMENT,
        NotificationCategory.GENERAL,
        NotificationPriority.NORMAL,
        "Title",
        "Content",
        TargetScopeType.USERS,
        "0",
        null,
        null,
        null,
        10L,
        Set.of(),
        null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> publisherService.publish(command));
    assertEquals(NotificationErrorConstants.RECIPIENTS_EMPTY, ex.getStrCode());
  }

  @Test
  void publish_whenRecipientsExceedMaxLimit_throwsInvalidInput() {
    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.PGV_ANNOUNCEMENT,
        NotificationCategory.GENERAL,
        NotificationPriority.NORMAL,
        "Title",
        "Content",
        TargetScopeType.ALL,
        "ALL",
        null,
        null,
        null,
        10L,
        Set.of(1L, 2L, 3L, 4L, 5L, 6L),
        null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> publisherService.publish(command));
    assertEquals(NotificationErrorConstants.RECIPIENTS_LIMIT_EXCEEDED, ex.getStrCode());
  }
}
