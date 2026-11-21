package org.ptit.meeting.modules.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AcademicCatalogCreateRequest(
    @NotBlank(message = "{validation.student-structure.required}")
    @Pattern(regexp = "[A-Za-z0-9_-]{1,30}", message = "{validation.student-structure.code.invalid}")
    String code,
    @NotBlank(message = "{validation.student-structure.required}")
    @Size(max = 150, message = "{validation.student-structure.name.size}") String name
) {}
