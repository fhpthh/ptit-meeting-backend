package org.ptit.meeting.modules.notification.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.timetable.event.LecturerAssignedEvent;
import org.ptit.meeting.modules.timetable.event.StudentEnrolledInClassSectionEvent;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class TimetableNotificationEventListenerTest {

  @Mock
  private NotificationPublisherService publisherService;
  @Mock
  private MessageSource messageSource;

  private TimetableNotificationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new TimetableNotificationEventListener(
        publisherService,
        messageSource,
        new NotificationProperties()
    );
  }

  @Test
  void onLecturerAssigned_publishesScheduleChangeNotification() {
    when(messageSource.getMessage(eq("notification.event.lecturer-assigned.title"), any(), any(Locale.class)))
        .thenReturn("Assigned INT1434-01");
    when(messageSource.getMessage(eq("notification.event.lecturer-assigned.content"), any(), any(Locale.class)))
        .thenReturn("Assigned to INT1434-01 - Web");

    listener.onLecturerAssigned(new LecturerAssignedEvent(10L, "INT1434-01", "Web", 50L));

    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(NotificationType.SCHEDULE_CHANGE, captor.getValue().type());
    assertEquals(Set.of(50L), captor.getValue().recipientUserIds());
  }

  @Test
  void onStudentsEnrolled_publishesScheduleChangeNotificationToNewStudents() {
    when(messageSource.getMessage(eq("notification.event.student-enrolled.title"), any(), any(Locale.class)))
        .thenReturn("Enrolled INT1434-01");
    when(messageSource.getMessage(eq("notification.event.student-enrolled.content"), any(), any(Locale.class)))
        .thenReturn("Added to INT1434-01");

    listener.onStudentsEnrolled(new StudentEnrolledInClassSectionEvent(
        10L, "INT1434-01", Set.of(201L, 202L)
    ));

    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(NotificationType.SCHEDULE_CHANGE, captor.getValue().type());
    assertEquals(Set.of(201L, 202L), captor.getValue().recipientUserIds());
  }
}
