package org.ptit.meeting.modules.timetable.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SemesterUpdateRequest(
    @NotBlank(message = "{validation.timetable.semester.code.required}")
    @Size(max = 50, message = "{validation.timetable.semester.code.size}")
    String code,

    @NotBlank(message = "{validation.timetable.semester.name.required}")
    @Size(max = 100, message = "{validation.timetable.semester.name.size}")
    String name,

    @NotBlank(message = "{validation.timetable.semester.academic-year.required}")
    @Size(max = 20, message = "{validation.timetable.semester.academic-year.size}")
    String academicYear,

    @NotNull(message = "{validation.timetable.semester.semester-num.required}")
    @Min(value = 1, message = "{validation.timetable.semester.semester-num.invalid}")
    @Max(value = 3, message = "{validation.timetable.semester.semester-num.invalid}")
    Integer semesterNum,

    @NotNull(message = "{validation.timetable.semester.start-date.required}")
    LocalDate startDate,

    @NotNull(message = "{validation.timetable.semester.end-date.required}")
    LocalDate endDate,

    boolean isActive
) {
}