package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.ptit.meeting.modules.notification.repository.NotificationPreferenceRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.service.impl.OnlineClassReminderServiceImpl;
import org.ptit.meeting.modules.timetable.dto.internal.UpcomingOnlineSessionDto;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.ptit.meeting.modules.timetable.service.OnlineSessionReminderQueryService;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class OnlineClassReminderServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Mock
  private OnlineSessionReminderQueryService sessionQueryService;
  @Mock
  private ClassSectionAudienceService classSectionAudienceService;
  @Mock
  private NotificationPreferenceRepository preferenceRepository;
  @Mock
  private NotificationRecipientRepository recipientRepository;
  @Mock
  private NotificationPublisherService publisherService;
  @Mock
  private MessageSource messageSource;

  private NotificationProperties properties;
  private OnlineClassReminderServiceImpl reminderService;

  @BeforeEach
  void setUp() {
    properties = new NotificationProperties();
    properties.setMinReminderMinutes(30);
    properties.setDefaultReminderMinutes(30);
    properties.setMaxReminderMinutes(120);
    properties.setScheduleZoneId("Asia/Ho_Chi_Minh");
    properties.setDefaultLocale("vi");

    // Fixed clock: 2026-11-27 08:20 Asia/Ho_Chi_Minh (40 minutes before 09:00)
    Instant fixedNow = LocalDateTime.of(2026, 11, 27, 8, 20).atZone(ZONE).toInstant();
    Clock fixedClock = Clock.fixed(fixedNow, ZONE);

    reminderService = new OnlineClassReminderServiceImpl(
        sessionQueryService,
        classSectionAudienceService,
        preferenceRepository,
        recipientRepository,
        publisherService,
        messageSource,
        properties,
        fixedClock
    );
  }

  @Test
  void processUpcomingOnlineSessionReminders_respectsPerUserLeadTimeAndIdempotency() {
    UpcomingOnlineSessionDto session = new UpcomingOnlineSessionDto(
        100L, "session-pub-100", 20L, 10L, "INT1434-01", "01",
        "INT1434", "Lap trinh Web", 50L, "GV A", "Online Room 01", "room-pub-1",
        LocalDate.of(2026, 11, 27),
        LocalDateTime.of(2026, 11, 27, 9, 0),
        LocalDateTime.of(2026, 11, 27, 10, 50)
    );
    when(sessionQueryService.findUpcomingOnlineSessions(any(), any()))
        .thenReturn(List.of(session));
    // User 1: already notified, User 2: wants 45m lead (eligible at 08:20), User 3: default 30m (not yet eligible at 08:20)
    when(classSectionAudienceService.findActiveMemberUserIds(List.of(10L)))
        .thenReturn(Set.of(1L, 2L, 3L));
    when(recipientRepository.findNotifiedUserIds(
        NotificationType.CLASS_REMINDER,
        OnlineClassReminderServiceImpl.REFERENCE_TYPE_MEETING_SESSION,
        "session-pub-100"
    )).thenReturn(Set.of(1L));

    NotificationPreference user2Pref = NotificationPreference.builder()
        .userId(2L)
        .inAppEnabled(true)
        .zaloEnabled(false)
        .reminderMinutesBefore(45)
        .build();
    when(preferenceRepository.findAllByUserIdIn(Set.of(2L, 3L)))
        .thenReturn(List.of(user2Pref));

    when(messageSource.getMessage(eq("notification.online-class-reminder.title"), any(), any(Locale.class)))
        .thenReturn("[Nhắc lịch học trực tuyến] Lap trinh Web (INT1434-01)");
    when(messageSource.getMessage(eq("notification.online-class-reminder.content"), any(), any(Locale.class)))
        .thenReturn("Lớp học trực tuyến bắt đầu sau 40 phút.");

    Notification saved = Notification.builder().title("title").build();
    saved.setId("noti-rem-1");
    when(publisherService.publish(any(NotificationPublishCommand.class)))
        .thenReturn(new NotificationPublishResult(saved, 1L, false));

    int notified = reminderService.processUpcomingOnlineSessionReminders();

    assertEquals(1, notified);
    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService, times(1)).publish(captor.capture());
    NotificationPublishCommand command = captor.getValue();
    assertEquals(NotificationType.CLASS_REMINDER, command.type());
    assertEquals(Set.of(2L), command.recipientUserIds());
    assertEquals(Set.of(DeliveryChannel.IN_APP), command.channels());
    assertEquals("session-pub-100", command.referenceId());
  }

  @Test
  void processUpcomingOnlineSessionReminders_whenAllMembersAlreadyNotified_doesNotPublish() {
    UpcomingOnlineSessionDto session = new UpcomingOnlineSessionDto(
        101L, "session-pub-101", 21L, 11L, "INT1434-02", "02",
        "INT1434", "Lap trinh Web", 50L, "GV A", "Online Room 02", "room-pub-2",
        LocalDate.of(2026, 11, 27),
        LocalDateTime.of(2026, 11, 27, 8, 45),
        LocalDateTime.of(2026, 11, 27, 10, 35)
    );
    when(sessionQueryService.findUpcomingOnlineSessions(any(), any()))
        .thenReturn(List.of(session));
    when(classSectionAudienceService.findActiveMemberUserIds(List.of(11L)))
        .thenReturn(Set.of(1L, 2L));
    when(recipientRepository.findNotifiedUserIds(
        NotificationType.CLASS_REMINDER,
        OnlineClassReminderServiceImpl.REFERENCE_TYPE_MEETING_SESSION,
        "session-pub-101"
    )).thenReturn(Set.of(1L, 2L));

    int notified = reminderService.processUpcomingOnlineSessionReminders();

    assertEquals(0, notified);
    verify(publisherService, never()).publish(any());
  }
}
