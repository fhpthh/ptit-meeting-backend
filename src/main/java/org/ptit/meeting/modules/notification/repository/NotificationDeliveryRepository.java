package org.ptit.meeting.modules.notification.repository;

import java.util.List;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryStatus;
import org.ptit.meeting.modules.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

  List<NotificationDelivery> findAllByNotificationIdAndStatus(
      String notificationId,
      DeliveryStatus status
  );
}
