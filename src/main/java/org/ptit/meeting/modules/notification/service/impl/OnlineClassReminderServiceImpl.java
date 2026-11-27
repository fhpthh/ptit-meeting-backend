package org.ptit.meeting.modules.notification.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.constant.enums.TargetScopeType;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.ptit.meeting.modules.notification.repository.NotificationPreferenceRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.notification.service.OnlineClassReminderService;
import org.ptit.meeting.modules.timetable.dto.internal.UpcomingOnlineSessionDto;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.ptit.meeting.modules.timetable.service.OnlineSessionReminderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OnlineClassReminderServiceImpl implements OnlineClassReminderService {

  public static final String REFERENCE_TYPE_MEETING_SESSION = "MEETING_SESSION";
  private static final String TITLE_MESSAGE_KEY = "notification.online-class-reminder.title";
  private static final String CONTENT_MESSAGE_KEY = "notification.online-class-reminder.content";
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final OnlineSessionReminderQueryService sessionQueryService;
  private final ClassSectionAudienceService classSectionAudienceService;
  private final NotificationPreferenceRepository preferenceRepository;
  private final NotificationRecipientRepository recipientRepository;
  private final NotificationPublisherService publisherService;
  private final MessageSource messageSource;
  private final NotificationProperties properties;
  private final Clock clock;

  @Autowired
  public OnlineClassReminderServiceImpl(
      OnlineSessionReminderQueryService sessionQueryService,
      ClassSectionAudienceService classSectionAudienceService,
      NotificationPreferenceRepository preferenceRepository,
      NotificationRecipientRepository recipientRepository,
      NotificationPublisherService publisherService,
      MessageSource messageSource,
      NotificationProperties properties
  ) {
    this(
        sessionQueryService,
        classSectionAudienceService,
        preferenceRepository,
        recipientRepository,
        publisherService,
        messageSource,
        properties,
        Clock.systemDefaultZone()
    );
  }

  public OnlineClassReminderServiceImpl(
      OnlineSessionReminderQueryService sessionQueryService,
      ClassSectionAudienceService classSectionAudienceService,
      NotificationPreferenceRepository preferenceRepository,
      NotificationRecipientRepository recipientRepository,
      NotificationPublisherService publisherService,
      MessageSource messageSource,
      NotificationProperties properties,
      Clock clock
  ) {
    this.sessionQueryService = sessionQueryService;
    this.classSectionAudienceService = classSectionAudienceService;
    this.preferenceRepository = preferenceRepository;
    this.recipientRepository = recipientRepository;
    this.publisherService = publisherService;
    this.messageSource = messageSource;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public int processUpcomingOnlineSessionReminders() {
    ZoneId zoneId = ZoneId.of(properties.getScheduleZoneId());
    LocalDateTime now = LocalDateTime.now(clock.withZone(zoneId));
    int maxLeadMinutes = Math.max(properties.getMinReminderMinutes(),
        properties.getMaxReminderMinutes());
    LocalDateTime windowEnd = now.plusMinutes(maxLeadMinutes);

    List<UpcomingOnlineSessionDto> upcomingSessions = sessionQueryService
        .findUpcomingOnlineSessions(now, windowEnd);
    if (upcomingSessions.isEmpty()) {
      return 0;
    }

    int totalNotified = 0;
    for (UpcomingOnlineSessionDto session : upcomingSessions) {
      totalNotified += processSession(session, now);
    }
    return totalNotified;
  }

  private int processSession(UpcomingOnlineSessionDto session, LocalDateTime now) {
    Set<Long> memberUserIds = classSectionAudienceService
        .findActiveMemberUserIds(List.of(session.classSectionId()));
    if (memberUserIds.isEmpty()) {
      return 0;
    }

    Set<Long> alreadyNotified = recipientRepository.findNotifiedUserIds(
        NotificationType.CLASS_REMINDER,
        REFERENCE_TYPE_MEETING_SESSION,
        session.sessionPublicId()
    );

    Set<Long> pendingUsers = memberUserIds.stream()
        .filter(userId -> !alreadyNotified.contains(userId))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (pendingUsers.isEmpty()) {
      return 0;
    }

    Map<Long, NotificationPreference> preferencesByUser = preferenceRepository
        .findAllByUserIdIn(pendingUsers)
        .stream()
        .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()));

    Map<ReminderGroupKey, Set<Long>> groupedRecipients = new LinkedHashMap<>();
    for (Long userId : pendingUsers) {
      NotificationPreference preference = preferencesByUser.get(userId);
      int effectiveLeadMinutes = resolveEffectiveLeadMinutes(preference);
      if (session.scheduledStart().isAfter(now.plusMinutes(effectiveLeadMinutes))) {
        continue;
      }

      Set<DeliveryChannel> channels = resolveChannels(preference);
      if (channels.isEmpty()) {
        continue;
      }

      ReminderGroupKey key = new ReminderGroupKey(effectiveLeadMinutes, channels);
      groupedRecipients.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(userId);
    }

    if (groupedRecipients.isEmpty()) {
      return 0;
    }

    long minutesUntilStart = Math.max(1L, Duration.between(now, session.scheduledStart()).toMinutes());
    Locale locale = Locale.forLanguageTag(properties.getDefaultLocale());
    String title = messageSource.getMessage(
        TITLE_MESSAGE_KEY,
        new Object[]{session.subjectName(), session.classCode()},
        locale
    );
    String content = messageSource.getMessage(
        CONTENT_MESSAGE_KEY,
        new Object[]{
            session.subjectName(),
            session.classCode(),
            minutesUntilStart,
            session.scheduledStart().format(TIME_FORMATTER),
            session.scheduledEnd().format(TIME_FORMATTER),
            session.roomName()
        },
        locale
    );

    int notifiedCount = 0;
    for (Map.Entry<ReminderGroupKey, Set<Long>> entry : groupedRecipients.entrySet()) {
      ReminderGroupKey key = entry.getKey();
      Set<Long> recipients = entry.getValue();
      String dedupeKey = buildDedupeKey(session.sessionId(), key);

      NotificationPublishCommand command = new NotificationPublishCommand(
          NotificationType.CLASS_REMINDER,
          NotificationCategory.TIMETABLE,
          NotificationPriority.HIGH,
          title,
          content,
          TargetScopeType.CLASS_SECTION,
          String.valueOf(session.classSectionId()),
          REFERENCE_TYPE_MEETING_SESSION,
          session.sessionPublicId(),
          dedupeKey,
          null,
          recipients,
          key.channels()
      );
      NotificationPublishResult result = publisherService.publish(command);
      if (!result.deduplicated()) {
        notifiedCount += (int) result.totalRecipients();
      }
    }

    log.info("Processed online class reminder for sessionId={} ({}), notified={}",
        session.sessionId(), session.classCode(), notifiedCount);
    return notifiedCount;
  }

  private int resolveEffectiveLeadMinutes(NotificationPreference preference) {
    int configured = preference != null
        ? preference.getReminderMinutesBefore()
        : properties.getDefaultReminderMinutes();
    int clampedMin = Math.max(properties.getMinReminderMinutes(), configured);
    return Math.min(clampedMin, properties.getMaxReminderMinutes());
  }

  private Set<DeliveryChannel> resolveChannels(NotificationPreference preference) {
    EnumSet<DeliveryChannel> channels = EnumSet.noneOf(DeliveryChannel.class);
    boolean inApp = preference == null || preference.isInAppEnabled();
    boolean zalo = preference == null || preference.isZaloEnabled();
    if (inApp) {
      channels.add(DeliveryChannel.IN_APP);
    }
    if (zalo) {
      channels.add(DeliveryChannel.ZALO);
    }
    return channels;
  }

  private String buildDedupeKey(Long sessionId, ReminderGroupKey key) {
    String channelSuffix = key.channels().stream()
        .map(Enum::name)
        .sorted()
        .collect(Collectors.joining("_"));
    return "ONLINE_CLASS_REMINDER:" + sessionId + ":" + key.leadMinutes() + ":" + channelSuffix;
  }

  private record ReminderGroupKey(int leadMinutes, Set<DeliveryChannel> channels) {
  }
}
