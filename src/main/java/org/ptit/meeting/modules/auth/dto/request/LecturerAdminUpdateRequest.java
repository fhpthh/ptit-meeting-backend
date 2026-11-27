package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LecturerAdminUpdateRequest(
    @NotBlank(message = "{validation.lecturer-admin.code.required}")
    @Pattern(regexp = "[A-Za-z0-9_-]{1,30}", message = "{validation.lecturer-admin.code.invalid}")
    String code,

    @NotBlank(message = "{validation.lecturer-admin.fullname.required}")
    @Size(max = 150)
    String fullName,

    @Size(max = 150)
    String email,

    @Size(max = 150)
    String department
) {}
