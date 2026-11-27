package org.ptit.meeting.modules.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(
    @NotNull(message = "{validation.notification.preference.in-app.required}")
    Boolean inAppEnabled,

    @NotNull(message = "{validation.notification.preference.zalo.required}")
    Boolean zaloEnabled,

    @NotNull(message = "{validation.notification.preference.reminder-minutes.required}")
    Integer reminderMinutesBefore,

    @NotNull(message = "{validation.notification.preference.receive-pgv-zalo.required}")
    Boolean receivePgvViaZalo,

    @NotNull(message = "{validation.notification.preference.receive-lecturer-zalo.required}")
    Boolean receiveLecturerViaZalo
) {
}
