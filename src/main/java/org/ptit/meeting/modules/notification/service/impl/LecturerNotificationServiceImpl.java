package org.ptit.meeting.modules.notification.service.impl;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.dto.request.LecturerAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.mapper.NotificationMapper;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.LecturerNotificationService;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LecturerNotificationServiceImpl extends AbstractAnnouncementService
    implements LecturerNotificationService {

  private static final String CLASS_SECTION_REFERENCE_TYPE = "CLASS_SECTION";

  private final ClassSectionAudienceService classSectionAudienceService;

  public LecturerNotificationServiceImpl(
      NotificationRepository notificationRepository,
      NotificationPublisherService publisherService,
      NotificationMapper notificationMapper,
      NotificationProperties properties,
      ClassSectionAudienceService classSectionAudienceService
  ) {
    super(notificationRepository, publisherService, notificationMapper, properties);
    this.classSectionAudienceService = classSectionAudienceService;
  }

  @Override
  @Transactional
  public AnnouncementResponse createAnnouncement(
      Long lecturerUserId,
      LecturerAnnouncementCreateRequest request
  ) {
    Set<Long> sectionIds = request.classSectionIds().stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (sectionIds.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.TARGET_CLASS_SECTIONS_REQUIRED);
    }

    Set<Long> enrolledStudentIds = classSectionAudienceService
        .findOwnedClassSectionStudentIds(lecturerUserId, sectionIds);

    Set<Long> targetRecipients = resolveStudentSubset(enrolledStudentIds, request.studentIds());
    targetRecipients.remove(lecturerUserId);

    String targetRef = sectionIds.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    String referenceId = sectionIds.size() == 1
        ? String.valueOf(sectionIds.iterator().next())
        : targetRef;

    NotificationPublishCommand command = new NotificationPublishCommand(
        NotificationType.LECTURER_ANNOUNCEMENT,
        request.category() != null ? request.category() : NotificationCategory.COURSE,
        request.priority(),
        request.title(),
        request.content(),
        TargetScopeType.CLASS_SECTION,
        targetRef,
        CLASS_SECTION_REFERENCE_TYPE,
        referenceId,
        null,
        lecturerUserId,
        targetRecipients,
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
  public PageResponseDTO<AnnouncementResponse> listOwnAnnouncements(
      Long lecturerUserId,
      NotificationCategory category,
      NotificationPriority priority,
      String keyword,
      int page,
      int size
  ) {
    return searchSentAnnouncements(
        lecturerUserId,
        NotificationType.LECTURER_ANNOUNCEMENT,
        category,
        priority,
        keyword,
        page,
        size
    );
  }

  @Override
  @Transactional(readOnly = true)
  public AnnouncementResponse getOwnAnnouncementDetail(Long lecturerUserId, String notificationId) {
    return getAnnouncementDetailWithAccessCheck(lecturerUserId, notificationId);
  }

  @Override
  @Transactional
  public void deleteOwnAnnouncement(Long lecturerUserId, String notificationId) {
    softDeleteAnnouncementWithAccessCheck(lecturerUserId, notificationId);
  }

  @Override
  protected void verifyAccess(
      Long actorUserId,
      NotificationType type,
      Long createdBy,
      String notificationId
  ) {
    if (type != NotificationType.LECTURER_ANNOUNCEMENT
        || createdBy == null
        || !createdBy.equals(actorUserId)) {
      throw BusinessException.unauthorized(
          NotificationErrorConstants.ACCESS_DENIED,
          notificationId
      );
    }
  }

  private Set<Long> resolveStudentSubset(Set<Long> enrolledStudentIds,
      java.util.List<Long> requestedStudentIds) {
    if (requestedStudentIds == null || requestedStudentIds.isEmpty()) {
      return new LinkedHashSet<>(enrolledStudentIds);
    }
    Set<Long> subset = new LinkedHashSet<>();
    for (Long studentId : requestedStudentIds) {
      if (studentId == null) {
        continue;
      }
      if (!enrolledStudentIds.contains(studentId)) {
        throw BusinessException.invalidInput(
            NotificationErrorConstants.STUDENT_NOT_IN_CLASS_SECTIONS,
            studentId
        );
      }
      subset.add(studentId);
    }
    return subset;
  }
}
