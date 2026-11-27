package org.ptit.meeting.modules.notification.service.impl;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.notification.adapter.ChannelDeliveryResult;
import org.ptit.meeting.modules.notification.adapter.NotificationChannelAdapter;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryStatus;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationDelivery;
import org.ptit.meeting.modules.notification.repository.NotificationDeliveryRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.NotificationDispatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

  private final NotificationRepository notificationRepository;
  private final NotificationDeliveryRepository deliveryRepository;
  private final Map<DeliveryChannel, NotificationChannelAdapter> adapters;

  public NotificationDispatchServiceImpl(
      NotificationRepository notificationRepository,
      NotificationDeliveryRepository deliveryRepository,
      List<NotificationChannelAdapter> adapterList
  ) {
    this.notificationRepository = notificationRepository;
    this.deliveryRepository = deliveryRepository;
    Map<DeliveryChannel, NotificationChannelAdapter> map = new EnumMap<>(DeliveryChannel.class);
    for (NotificationChannelAdapter adapter : adapterList) {
      map.put(adapter.channel(), adapter);
    }
    this.adapters = Map.copyOf(map);
  }

  @Override
  @Transactional
  public void dispatchPendingDeliveries(String notificationId) {
    Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
        .orElse(null);
    if (notification == null) {
      return;
    }

    List<NotificationDelivery> pendingDeliveries = deliveryRepository
        .findAllByNotificationIdAndStatus(notificationId, DeliveryStatus.PENDING);
    if (pendingDeliveries.isEmpty()) {
      return;
    }

    for (NotificationDelivery delivery : pendingDeliveries) {
      NotificationChannelAdapter adapter = adapters.get(delivery.getChannel());
      delivery.setAttemptCount(delivery.getAttemptCount() + 1);

      if (adapter == null) {
        delivery.setStatus(DeliveryStatus.SKIPPED);
        delivery.setErrorMessage("No channel adapter registered for " + delivery.getChannel());
        continue;
      }

      try {
        ChannelDeliveryResult result = adapter.deliver(notification, delivery);
        applyDeliveryResult(delivery, result);
      } catch (Exception ex) {
        log.error("Failed to dispatch notification {} on channel {} for user {}",
            notificationId, delivery.getChannel(), delivery.getUserId(), ex);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setErrorMessage(truncate(ex.getMessage(), 500));
      }
    }

    deliveryRepository.saveAll(pendingDeliveries);
  }

  private void applyDeliveryResult(NotificationDelivery delivery, ChannelDeliveryResult result) {
    delivery.setStatus(result.status());
    delivery.setProviderMessageId(result.providerMessageId());
    delivery.setErrorCode(result.errorCode());
    delivery.setErrorMessage(truncate(result.errorMessage(), 500));
    if (result.status() == DeliveryStatus.SENT) {
      delivery.setSentAt(Instant.now());
    }
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
