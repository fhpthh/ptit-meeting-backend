package org.ptit.meeting.modules.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.ptit.meeting.modules.notification.constant.enums.DeliveryChannel;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;

public record LecturerAnnouncementCreateRequest(
    @NotBlank(message = "{validation.notification.title.required}")
    @Size(max = 255, message = "{validation.notification.title.size}")
    String title,

    @NotBlank(message = "{validation.notification.content.required}")
    @Size(max = 10000, message = "{validation.notification.content.size}")
    String content,

    NotificationCategory category,

    NotificationPriority priority,

    @NotEmpty(message = "{validation.notification.class-sections.required}")
    List<@NotNull(message = "{validation.notification.id.required}") @Positive(message = "{validation.notification.id.positive}") Long> classSectionIds,

    List<@NotNull(message = "{validation.notification.id.required}") @Positive(message = "{validation.notification.id.positive}") Long> studentIds,

    List<@NotBlank(message = "{validation.notification.email.invalid}") @Email(message = "{validation.notification.email.invalid}") String> studentEmails,

    Set<DeliveryChannel> channels
) {
}
