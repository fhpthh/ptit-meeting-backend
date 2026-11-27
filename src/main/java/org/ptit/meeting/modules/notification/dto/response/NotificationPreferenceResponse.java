package org.ptit.meeting.modules.notification.dto.response;

public record NotificationPreferenceResponse(
    boolean inAppEnabled,
    boolean zaloEnabled,
    int reminderMinutesBefore,
    boolean receivePgvViaZalo,
    boolean receiveLecturerViaZalo
) {
}
