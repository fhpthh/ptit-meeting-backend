package org.ptit.meeting.modules.timetable.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClassSectionLecturerUpdateRequest(
    @NotNull(message = "{validation.timetable.lecturer.required}")
    @Positive(message = "{validation.timetable.lecturer.invalid}") Long lecturerId
) {
}
