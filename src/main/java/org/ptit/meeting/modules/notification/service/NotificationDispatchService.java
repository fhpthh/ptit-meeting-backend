package org.ptit.meeting.modules.notification.service;

/**
 * Dispatches pending notification deliveries through registered channel adapters.
 */
public interface NotificationDispatchService {

  void dispatchPendingDeliveries(String notificationId);
}
