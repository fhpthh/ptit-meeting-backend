package org.ptit.meeting.modules.notification.adapter;

import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationDelivery;
import org.springframework.stereotype.Component;

/**
 * In-app delivery is persisted synchronously as {@code NotificationRecipient} rows
 * during publishing, so dispatch immediately marks the delivery as sent.
 */
@Component
public class InAppNotificationChannelAdapter implements NotificationChannelAdapter {

  @Override
  public DeliveryChannel channel() {
    return DeliveryChannel.IN_APP;
  }

  @Override
  public ChannelDeliveryResult deliver(Notification notification, NotificationDelivery delivery) {
    return ChannelDeliveryResult.sent(notification.getId());
  }
}
