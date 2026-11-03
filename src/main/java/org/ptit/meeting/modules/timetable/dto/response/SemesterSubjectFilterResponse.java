package org.ptit.meeting.modules.timetable.dto.response;

import java.util.List;

public record SemesterSubjectFilterResponse(
    Long semesterId,
    List<String> departments,
    List<String> cohorts,
    List<String> managementClasses
) {
}
