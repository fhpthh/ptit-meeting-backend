package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;

public record StudentAdminBulkStatusRequest(
    @NotNull(message = "{validation.student-admin.required}")
    @Size(min = 1, max = 100, message = "{validation.student-admin.ids.size}")
    List<@NotNull(message = "{validation.student-admin.required}")
         @Positive(message = "{validation.student-admin.id.positive}") Long> ids,
    @NotNull(message = "{validation.student-admin.required}") UserStatus status
) {}
