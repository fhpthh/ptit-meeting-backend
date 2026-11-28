package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.dto.request.LecturerAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.impl.LecturerNotificationServiceImpl;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;

@ExtendWith(MockitoExtension.class)
class LecturerNotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private NotificationPublisherService publisherService;
  @Mock
  private ClassSectionAudienceService classSectionAudienceService;
  @Mock
  private UserService userService;

  private LecturerNotificationServiceImpl lecturerService;

  @BeforeEach
  void setUp() {
    lecturerService = new LecturerNotificationServiceImpl(
        notificationRepository,
        publisherService,
        new NotificationMapper(),
        new NotificationProperties(),
        classSectionAudienceService,
        userService
    );
  }

  @Test
  void createAnnouncement_whenOwnedClassSection_publishesToEnrolledStudents() {
    when(classSectionAudienceService.findOwnedClassSectionStudentIds(50L, Set.of(100L)))
        .thenReturn(Set.of(201L, 202L));

    Notification saved = Notification.builder()
        .type(NotificationType.LECTURER_ANNOUNCEMENT)
        .category(NotificationCategory.COURSE)
        .priority(NotificationPriority.NORMAL)
        .title("Lab 3 reminder")
        .content("Submit before 23:59")
        .targetScope(TargetScopeType.CLASS_SECTION)
        .targetRef("100")
        .createdBy(50L)
        .publishedAt(Instant.now())
        .build();
    saved.setId("noti-lec-1");
    when(publisherService.publish(any(NotificationPublishCommand.class)))
        .thenReturn(new NotificationPublishResult(saved, 2L, false));

    LecturerAnnouncementCreateRequest request = new LecturerAnnouncementCreateRequest(
        "Lab 3 reminder",
        "Submit before 23:59",
        null,
        NotificationPriority.NORMAL,
        List.of(100L),
        null,
        null,
        null
    );

    AnnouncementResponse response = lecturerService.createAnnouncement(50L, request);

    assertEquals("noti-lec-1", response.id());
    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(NotificationType.LECTURER_ANNOUNCEMENT, captor.getValue().type());
    assertEquals(NotificationCategory.COURSE, captor.getValue().category());
    assertEquals(Set.of(201L, 202L), captor.getValue().recipientUserIds());
  }

  @Test
  void createAnnouncement_whenSubsetStudentNotEnrolled_throwsInvalidInput() {
    when(classSectionAudienceService.findOwnedClassSectionStudentIds(50L, Set.of(100L)))
        .thenReturn(Set.of(201L, 202L));

    LecturerAnnouncementCreateRequest request = new LecturerAnnouncementCreateRequest(
        "Reminder",
        "Please attend",
        NotificationCategory.COURSE,
        NotificationPriority.NORMAL,
        List.of(100L),
        List.of(999L),
        null,
        null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> lecturerService.createAnnouncement(50L, request));
    assertEquals(NotificationErrorConstants.STUDENT_NOT_IN_CLASS_SECTIONS, ex.getStrCode());
  }

  @Test
  void createAnnouncement_whenSubsetByStudentEmail_publishesToMatchedEnrolledStudent() {
    when(classSectionAudienceService.findOwnedClassSectionStudentIds(50L, Set.of(100L)))
        .thenReturn(Set.of(201L, 202L));
    when(userService.findActiveUserIdsByEmails(Set.of("sv201@stu.ptit.edu.vn")))
        .thenReturn(Map.of("sv201@stu.ptit.edu.vn", 201L));

    Notification saved = Notification.builder()
        .type(NotificationType.LECTURER_ANNOUNCEMENT)
        .category(NotificationCategory.COURSE)
        .priority(NotificationPriority.NORMAL)
        .title("Private reminder")
        .content("Please submit report")
        .targetScope(TargetScopeType.CLASS_SECTION)
        .targetRef("100")
        .createdBy(50L)
        .publishedAt(Instant.now())
        .build();
    saved.setId("noti-lec-email-1");
    when(publisherService.publish(any(NotificationPublishCommand.class)))
        .thenReturn(new NotificationPublishResult(saved, 1L, false));

    LecturerAnnouncementCreateRequest request = new LecturerAnnouncementCreateRequest(
        "Private reminder",
        "Please submit report",
        NotificationCategory.COURSE,
        NotificationPriority.NORMAL,
        List.of(100L),
        null,
        List.of("SV201@stu.ptit.edu.vn"),
        null
    );

    AnnouncementResponse response = lecturerService.createAnnouncement(50L, request);

    assertEquals("noti-lec-email-1", response.id());
    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(Set.of(201L), captor.getValue().recipientUserIds());
  }

  @Test
  void deleteOwnAnnouncement_whenCreatedByAnotherLecturer_throwsAccessDenied() {
    Notification other = Notification.builder()
        .type(NotificationType.LECTURER_ANNOUNCEMENT)
        .createdBy(99L)
        .deleted(false)
        .build();
    other.setId("noti-other");
    when(notificationRepository.findByIdAndDeletedFalse("noti-other"))
        .thenReturn(Optional.of(other));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> lecturerService.deleteOwnAnnouncement(50L, "noti-other"));
    assertEquals(NotificationErrorConstants.ACCESS_DENIED, ex.getStrCode());
  }
}
