package org.ptit.meeting.modules.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.audience.AllUsersAudienceResolver;
import org.ptit.meeting.modules.notification.audience.AudienceResolverRegistry;
import org.ptit.meeting.modules.notification.audience.ClassSectionAudienceResolver;
import org.ptit.meeting.modules.notification.audience.DepartmentAudienceResolver;
import org.ptit.meeting.modules.notification.audience.ExplicitUsersAudienceResolver;
import org.ptit.meeting.modules.notification.audience.RoleAudienceResolver;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.dto.request.AdminAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.impl.NotificationAdminServiceImpl;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;

@ExtendWith(MockitoExtension.class)
class NotificationAdminServiceTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private NotificationPublisherService publisherService;
  @Mock
  private UserService userService;
  @Mock
  private ClassSectionAudienceService classSectionAudienceService;

  private NotificationAdminServiceImpl adminService;

  @BeforeEach
  void setUp() {
    AudienceResolverRegistry registry = new AudienceResolverRegistry(List.of(
        new AllUsersAudienceResolver(userService),
        new RoleAudienceResolver(userService),
        new DepartmentAudienceResolver(userService),
        new ClassSectionAudienceResolver(classSectionAudienceService),
        new ExplicitUsersAudienceResolver(userService)
    ));
    adminService = new NotificationAdminServiceImpl(
        notificationRepository,
        publisherService,
        new NotificationMapper(),
        new NotificationProperties(),
        registry,
        userService,
        classSectionAudienceService
    );
  }

  @Test
  void createAnnouncement_whenRoleScope_resolvesStudentsAndPublishes() {
    when(userService.findActiveUserIdsByAccountTypes(List.of(AccountType.STUDENT)))
        .thenReturn(List.of(1L, 10L, 11L));

    Notification saved = Notification.builder()
        .type(NotificationType.PGV_ANNOUNCEMENT)
        .category(NotificationCategory.ACADEMIC)
        .priority(NotificationPriority.HIGH)
        .title("Registration open")
        .content("Course registration opens tomorrow")
        .targetScope(TargetScopeType.ROLE)
        .targetRef("STUDENT")
        .createdBy(1L)
        .publishedAt(Instant.now())
        .build();
    saved.setId("noti-pgv-1");
    when(publisherService.publish(any(NotificationPublishCommand.class)))
        .thenReturn(new NotificationPublishResult(saved, 2L, false));

    AdminAnnouncementCreateRequest request = new AdminAnnouncementCreateRequest(
        "Registration open",
        "Course registration opens tomorrow",
        NotificationCategory.ACADEMIC,
        NotificationPriority.HIGH,
        TargetScopeType.ROLE,
        AccountType.STUDENT,
        null,
        null,
        null,
        null,
        Set.of(DeliveryChannel.IN_APP)
    );

    AnnouncementResponse response = adminService.createAnnouncement(1L, request);

    assertEquals("noti-pgv-1", response.id());
    assertEquals(2L, response.totalRecipients());
    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(NotificationType.PGV_ANNOUNCEMENT, captor.getValue().type());
    assertEquals(Set.of(10L, 11L), captor.getValue().recipientUserIds());
  }

  @Test
  void createAnnouncement_whenRoleScopeHasAdminTarget_throwsInvalidRole() {
    AdminAnnouncementCreateRequest request = new AdminAnnouncementCreateRequest(
        "Title",
        "Content",
        NotificationCategory.GENERAL,
        NotificationPriority.NORMAL,
        TargetScopeType.ROLE,
        AccountType.ADMIN,
        null,
        null,
        null,
        null,
        null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> adminService.createAnnouncement(1L, request));
    assertEquals(NotificationErrorConstants.TARGET_ROLE_INVALID, ex.getStrCode());
  }

  @Test
  void createAnnouncement_whenUsersScopeByEmails_resolvesActiveUserIds() {
    when(userService.findActiveUserIdsByEmails(Set.of("sv01@stu.ptit.edu.vn", "gv01@ptit.edu.vn")))
        .thenReturn(Map.of("sv01@stu.ptit.edu.vn", 21L, "gv01@ptit.edu.vn", 22L));

    Notification saved = Notification.builder()
        .type(NotificationType.PGV_ANNOUNCEMENT)
        .category(NotificationCategory.GENERAL)
        .priority(NotificationPriority.NORMAL)
        .title("Direct notice")
        .content("Please check your schedule")
        .targetScope(TargetScopeType.USERS)
        .targetRef("sv01@stu.ptit.edu.vn, gv01@ptit.edu.vn")
        .createdBy(1L)
        .publishedAt(Instant.now())
        .build();
    saved.setId("noti-email-1");
    when(publisherService.publish(any(NotificationPublishCommand.class)))
        .thenReturn(new NotificationPublishResult(saved, 2L, false));

    AdminAnnouncementCreateRequest request = new AdminAnnouncementCreateRequest(
        "Direct notice",
        "Please check your schedule",
        NotificationCategory.GENERAL,
        NotificationPriority.NORMAL,
        TargetScopeType.USERS,
        null,
        null,
        null,
        null,
        List.of("SV01@stu.ptit.edu.vn", "gv01@ptit.edu.vn"),
        Set.of(DeliveryChannel.IN_APP)
    );

    AnnouncementResponse response = adminService.createAnnouncement(1L, request);

    assertEquals("noti-email-1", response.id());
    ArgumentCaptor<NotificationPublishCommand> captor = ArgumentCaptor
        .forClass(NotificationPublishCommand.class);
    verify(publisherService).publish(captor.capture());
    assertEquals(Set.of(21L, 22L), captor.getValue().recipientUserIds());
  }

  @Test
  void createAnnouncement_whenUserEmailMissing_throwsNotFoundError() {
    when(userService.findActiveUserIdsByEmails(Set.of("missing@stu.ptit.edu.vn")))
        .thenReturn(Map.of());

    AdminAnnouncementCreateRequest request = new AdminAnnouncementCreateRequest(
        "Direct notice",
        "Body",
        NotificationCategory.GENERAL,
        NotificationPriority.NORMAL,
        TargetScopeType.USERS,
        null,
        null,
        null,
        null,
        List.of("missing@stu.ptit.edu.vn"),
        null
    );

    BusinessException ex = assertThrows(BusinessException.class,
        () -> adminService.createAnnouncement(1L, request));
    assertEquals(NotificationErrorConstants.TARGET_USER_EMAILS_NOT_FOUND, ex.getStrCode());
  }

  @Test
  void deleteAnnouncement_whenExists_marksNotificationAsDeleted() {
    Notification notification = Notification.builder()
        .type(NotificationType.PGV_ANNOUNCEMENT)
        .deleted(false)
        .build();
    notification.setId("noti-del-1");
    when(notificationRepository.findByIdAndDeletedFalse("noti-del-1"))
        .thenReturn(Optional.of(notification));

    adminService.deleteAnnouncement("noti-del-1");

    assertTrue(notification.isDeleted());
    verify(notificationRepository).save(notification);
  }
}
