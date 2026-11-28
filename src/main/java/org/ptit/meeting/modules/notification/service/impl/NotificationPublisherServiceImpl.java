package org.ptit.meeting.modules.notification.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.config.NotificationProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.notification.constant.NotificationErrorConstants;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryStatus;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishCommand;
import org.ptit.meeting.modules.notification.dto.internal.NotificationPublishResult;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationDelivery;
import org.ptit.meeting.modules.notification.entity.NotificationRecipient;
import org.ptit.meeting.modules.notification.repository.NotificationDeliveryRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.NotificationDispatchService;
import org.ptit.meeting.modules.notification.service.NotificationPublisherService;
import org.ptit.meeting.modules.notification.service.NotificationRealtimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NotificationPublisherServiceImpl implements NotificationPublisherService {

  private final NotificationRepository notificationRepository;
  private final NotificationRecipientRepository recipientRepository;
  private final NotificationDeliveryRepository deliveryRepository;
  private final NotificationDispatchService dispatchService;
  private final NotificationRealtimeService realtimeService;
  private final NotificationProperties properties;

  @Override
  @Transactional
  public NotificationPublishResult publish(NotificationPublishCommand command) {
    if (command.dedupeKey() != null && !command.dedupeKey().isBlank()) {
      Optional<Notification> existing = notificationRepository.findByDedupeKey(command.dedupeKey());
      if (existing.isPresent()) {
        return new NotificationPublishResult(existing.get(), 0L, true);
      }
    }

    Set<Long> recipients = sanitizeRecipients(command.recipientUserIds());
    if (recipients.isEmpty()) {
      throw BusinessException.invalidInput(NotificationErrorConstants.RECIPIENTS_EMPTY);
    }
    if (recipients.size() > properties.getMaxTargetUsers()) {
      throw BusinessException.invalidInput(
          NotificationErrorConstants.RECIPIENTS_LIMIT_EXCEEDED,
          properties.getMaxTargetUsers()
      );
    }

    Notification notification = Notification.builder()
        .type(command.type())
        .category(command.category() != null ? command.category() : NotificationCategory.GENERAL)
        .priority(command.priority() != null ? command.priority() : NotificationPriority.NORMAL)
        .title(command.title().trim())
        .content(command.content().trim())
        .targetScope(command.targetScope())
        .targetRef(command.targetRef())
        .referenceType(command.referenceType())
        .referenceId(command.referenceId())
        .dedupeKey(normalizeBlank(command.dedupeKey()))
        .createdBy(command.createdBy())
        .publishedAt(Instant.now())
        .deleted(false)
        .build();

    Notification saved = notificationRepository.save(notification);

    Set<DeliveryChannel> channels = resolveChannels(command.channels());
    List<NotificationRecipient> savedRecipients = List.of();
    if (channels.contains(DeliveryChannel.IN_APP)) {
      savedRecipients = persistRecipientsInBatches(saved.getId(), recipients);
    }

    Set<DeliveryChannel> externalChannels = channels.stream()
        .filter(channel -> channel != DeliveryChannel.IN_APP)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(DeliveryChannel.class)));

    if (!externalChannels.isEmpty()) {
      persistDeliveriesInBatches(saved.getId(), recipients, externalChannels);
    }

    // Push realtime SSE and dispatch external channels only after DB commit succeeds.
    schedulePostCommitDelivery(saved, savedRecipients, !externalChannels.isEmpty());

    return new NotificationPublishResult(saved, recipients.size(), false);
  }

  private Set<Long> sanitizeRecipients(Set<Long> rawRecipients) {
    if (rawRecipients == null || rawRecipients.isEmpty()) {
      return Set.of();
    }
    return rawRecipients.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<DeliveryChannel> resolveChannels(Set<DeliveryChannel> requestedChannels) {
    EnumSet<DeliveryChannel> resolved = EnumSet.of(DeliveryChannel.IN_APP);
    if (requestedChannels != null) {
      requestedChannels.stream()
          .filter(Objects::nonNull)
          .forEach(resolved::add);
    }
    return resolved;
  }

  private List<NotificationRecipient> persistRecipientsInBatches(
      String notificationId,
      Set<Long> userIds
  ) {
    int batchSize = Math.max(1, properties.getRecipientBatchSize());
    List<NotificationRecipient> allSaved = new ArrayList<>(userIds.size());
    List<NotificationRecipient> batch = new ArrayList<>(batchSize);
    for (Long userId : userIds) {
      batch.add(NotificationRecipient.builder()
          .notificationId(notificationId)
          .userId(userId)
          .read(false)
          .build());
      if (batch.size() >= batchSize) {
        allSaved.addAll(recipientRepository.saveAll(batch));
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      allSaved.addAll(recipientRepository.saveAll(batch));
    }
    return allSaved;
  }

  private void persistDeliveriesInBatches(
      String notificationId,
      Set<Long> userIds,
      Set<DeliveryChannel> channels
  ) {
    int batchSize = Math.max(1, properties.getRecipientBatchSize());
    List<NotificationDelivery> batch = new ArrayList<>(batchSize);
    for (DeliveryChannel channel : channels) {
      for (Long userId : userIds) {
        batch.add(NotificationDelivery.builder()
            .notificationId(notificationId)
            .userId(userId)
            .channel(channel)
            .status(DeliveryStatus.PENDING)
            .attemptCount(0)
            .build());
        if (batch.size() >= batchSize) {
          deliveryRepository.saveAll(batch);
          batch.clear();
        }
      }
    }
    if (!batch.isEmpty()) {
      deliveryRepository.saveAll(batch);
    }
  }

  private void schedulePostCommitDelivery(
      Notification notification,
      List<NotificationRecipient> savedRecipients,
      boolean hasExternalChannels
  ) {
    Runnable postCommitAction = () -> {
      if (!savedRecipients.isEmpty()) {
        realtimeService.pushToUsers(notification, savedRecipients);
      }
      if (hasExternalChannels) {
        dispatchService.dispatchPendingDeliveries(notification.getId());
      }
    };

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          postCommitAction.run();
        }
      });
    } else {
      postCommitAction.run();
    }
  }

  private String normalizeBlank(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }
}
