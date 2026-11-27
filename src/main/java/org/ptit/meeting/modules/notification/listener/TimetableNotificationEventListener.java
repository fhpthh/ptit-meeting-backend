package org.ptit.meeting.modules.notification.listener;

import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.timetable.event.LecturerAssignedEvent;
import org.ptit.meeting.modules.timetable.event.StudentEnrolledInClassSectionEvent;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableNotificationEventListener {

  private static final String REFERENCE_TYPE_CLASS_SECTION = "CLASS_SECTION";
  private static final String LECTURER_ASSIGNED_TITLE = "notification.event.lecturer-assigned.title";
  private static final String LECTURER_ASSIGNED_CONTENT = "notification.event.lecturer-assigned.content";
  private static final String STUDENT_ENROLLED_TITLE = "notification.event.student-enrolled.title";
  private static final String STUDENT_ENROLLED_CONTENT = "notification.event.student-enrolled.content";

  private final NotificationPublisherService publisherService;
  private final MessageSource messageSource;
  private final NotificationProperties properties;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onLecturerAssigned(LecturerAssignedEvent event) {
    if (event == null || event.lecturerId() == null) {
      return;
    }
    try {
      Locale locale = Locale.forLanguageTag(properties.getDefaultLocale());
      String title = messageSource.getMessage(
          LECTURER_ASSIGNED_TITLE,
          new Object[]{event.classCode()},
          locale
      );
      String content = messageSource.getMessage(
          LECTURER_ASSIGNED_CONTENT,
          new Object[]{event.classCode(), event.subjectName()},
          locale
      );

      NotificationPublishCommand command = new NotificationPublishCommand(
          NotificationType.SCHEDULE_CHANGE,
          NotificationCategory.TIMETABLE,
          NotificationPriority.NORMAL,
          title,
          content,
          TargetScopeType.USERS,
          String.valueOf(event.lecturerId()),
          REFERENCE_TYPE_CLASS_SECTION,
          String.valueOf(event.classSectionId()),
          null,
          null,
          Set.of(event.lecturerId()),
          Set.of(DeliveryChannel.IN_APP)
      );
      publisherService.publish(command);
    } catch (Exception ex) {
      log.error("Failed to publish lecturer-assigned notification for section {}",
          event.classSectionId(), ex);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onStudentsEnrolled(StudentEnrolledInClassSectionEvent event) {
    if (event == null || event.newlyEnrolledStudentIds() == null
        || event.newlyEnrolledStudentIds().isEmpty()) {
      return;
    }
    try {
      Locale locale = Locale.forLanguageTag(properties.getDefaultLocale());
      String title = messageSource.getMessage(
          STUDENT_ENROLLED_TITLE,
          new Object[]{event.classCode()},
          locale
      );
      String content = messageSource.getMessage(
          STUDENT_ENROLLED_CONTENT,
          new Object[]{event.classCode()},
          locale
      );

      NotificationPublishCommand command = new NotificationPublishCommand(
          NotificationType.SCHEDULE_CHANGE,
          NotificationCategory.TIMETABLE,
          NotificationPriority.NORMAL,
          title,
          content,
          TargetScopeType.CLASS_SECTION,
          String.valueOf(event.classSectionId()),
          REFERENCE_TYPE_CLASS_SECTION,
          String.valueOf(event.classSectionId()),
          null,
          null,
          event.newlyEnrolledStudentIds(),
          Set.of(DeliveryChannel.IN_APP)
      );
      publisherService.publish(command);
    } catch (Exception ex) {
      log.error("Failed to publish student-enrolled notification for section {}",
          event.classSectionId(), ex);
    }
  }
}
