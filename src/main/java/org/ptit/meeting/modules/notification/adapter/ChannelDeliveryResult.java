package org.ptit.meeting.modules.notification.adapter;

import org.ptit.meeting.modules.notification.constant.enums.DeliveryStatus;

public record ChannelDeliveryResult(
    DeliveryStatus status,
    String providerMessageId,
    String errorCode,
    String errorMessage
) {

  public static ChannelDeliveryResult sent(String providerMessageId) {
    return new ChannelDeliveryResult(DeliveryStatus.SENT, providerMessageId, null, null);
  }

  public static ChannelDeliveryResult skipped(String reason) {
    return new ChannelDeliveryResult(DeliveryStatus.SKIPPED, null, null, reason);
  }

  public static ChannelDeliveryResult failed(String errorCode, String errorMessage) {
    return new ChannelDeliveryResult(DeliveryStatus.FAILED, null, errorCode, errorMessage);
  }
}
