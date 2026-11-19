package org.ptit.meeting.modules.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdministrativeClassCreateRequest(
    @NotBlank(message = "{validation.student-structure.required}")
    @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "{validation.student-structure.class-code.invalid}")
    String code,
    @NotBlank(message = "{validation.student-structure.required}")
    @Size(max = 150, message = "{validation.student-structure.name.size}") String name,
    @NotNull(message = "{validation.student-structure.required}") @Positive Long campusId,
    @NotNull(message = "{validation.student-structure.required}") @Positive Long cohortId,
    @NotNull(message = "{validation.student-structure.required}") @Positive Long majorId
) {}
