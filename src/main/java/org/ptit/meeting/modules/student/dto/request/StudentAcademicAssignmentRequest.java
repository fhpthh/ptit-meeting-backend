package org.ptit.meeting.modules.student.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;

public record StudentAcademicAssignmentRequest(
    @NotNull(message = "{validation.student-structure.required}")
    @Positive(message = "{validation.student-admin.id.positive}") Long administrativeClassId,
    @NotNull(message = "{validation.student-structure.required}") StudyStatus studyStatus
) {}
