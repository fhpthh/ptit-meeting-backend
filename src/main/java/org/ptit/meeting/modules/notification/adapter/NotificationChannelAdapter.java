package org.ptit.meeting.modules.notification.adapter;

import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.entity.Notification;
import org.ptit.meeting.modules.notification.entity.NotificationDelivery;

/**
 * Channel delivery adapter contract. New delivery channels (e.g. Zalo, Email, FCM)
 * implement this interface without modifying the core notification publishing flow.
 */
public interface NotificationChannelAdapter {

  DeliveryChannel channel();

  ChannelDeliveryResult deliver(Notification notification, NotificationDelivery delivery);
}
