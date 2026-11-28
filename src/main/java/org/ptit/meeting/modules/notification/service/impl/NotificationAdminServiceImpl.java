package org.ptit.meeting.modules.notification.service.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.notification.audience.AudienceResolverRegistry;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.internal.AudienceResolutionRequest;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.dto.internal.ResolvedAudience;
import org.ptit.meeting.modules.notification.dto.request.AdminAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AdminAudienceOptionsResponse;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.NotificationAdminService;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationAdminServiceImpl extends AbstractAnnouncementService
    implements NotificationAdminService {

  private static final String ANNOUNCEMENT_REFERENCE_TYPE = "PGV_ANNOUNCEMENT";
  private static final int AUDIENCE_CLASS_OPTIONS_LIMIT = 100;

  private final AudienceResolverRegistry audienceResolverRegistry;
  private final UserService userService;
  private final ClassSectionAudienceService classSectionAudienceService;

  public NotificationAdminServiceImpl(
      NotificationRepository notificationRepository,
      NotificationPublisherService publisherService,
      NotificationMapper notificationMapper,
      NotificationProperties properties,
      AudienceResolverRegistry audienceResolverRegistry,
      UserService userService,
      ClassSectionAudienceService classSectionAudienceService
  ) {
    super(notificationRepository, publisherService, notificationMapper, properties);
    this.audienceResolverRegistry = audienceResolverRegistry;
    this.userService = userService;
    this.classSectionAudienceService = classSectionAudienceService;
  }

  @Override
  @Transactional
  public AnnouncementResponse createAnnouncement(
      Long actorUserId,
      AdminAnnouncementCreateRequest request
  ) {
    AudienceResolutionRequest audienceRequest = new AudienceResolutionRequest(
        request.targetScope(),
        request.targetRole(),
        request.targetDepartment(),
        request.classSectionIds(),
        request.userIds(),
        request.userEmails()
    );
    ResolvedAudience audience = audienceResolverRegistry.resolve(audienceRequest);
    // Exclude the sender so PGV/Admin does not receive their own published announcement.
    Set<Long> recipientUserIds = new LinkedHashSet<>(audience.recipientUserIds());
    recipientUserIds.remove(actorUserId);

    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.PGV_ANNOUNCEMENT,
        request.category(),
        request.priority(),
        request.title(),
        request.content(),
        request.targetScope(),
        audience.targetRef(),
        ANNOUNCEMENT_REFERENCE_TYPE,
        null,
        null,
        actorUserId,
        recipientUserIds,
        request.channels()
    );
    NotificationPublishResult published = publisherService.publish(command);
    return notificationMapper.toAnnouncementResponse(
        published.notification(),
        published.totalRecipients(),
        0L
    );
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<AnnouncementResponse> listAnnouncements(
      NotificationType type,
      NotificationCategory category,
      NotificationPriority priority,
      String keyword,
      int page,
      int size
  ) {
    return searchSentAnnouncements(null, type, category, priority, keyword, page, size);
  }

  @Override
  @Transactional(readOnly = true)
  public AnnouncementResponse getAnnouncementDetail(String notificationId) {
    return getAnnouncementDetailWithAccessCheck(null, notificationId);
  }

  @Override
  @Transactional
  public void deleteAnnouncement(String notificationId) {
    softDeleteAnnouncementWithAccessCheck(null, notificationId);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminAudienceOptionsResponse getAudienceOptions(String keyword) {
    return new AdminAudienceOptionsResponse(
        userService.findActiveDepartments(),
        classSectionAudienceService.findActiveClassSectionOptions(
            keyword,
            AUDIENCE_CLASS_OPTIONS_LIMIT
        )
    );
  }

  @Override
  protected void verifyAccess(
      Long actorUserId,
      NotificationType type,
      Long createdBy,
      String notificationId
  ) {
    // PGV (ROLE_STAFF / ROLE_ADMIN) has full visibility and revocation authority.
  }
}
