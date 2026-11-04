package org.ptit.meeting.modules.timetable.dto.response;

import org.ptit.meeting.common.dto.response.PageResponseDTO;

public record SemesterSubjectSectionsResponse(
    SemesterSubjectResponse subject,
    PageResponseDTO<ClassSectionSummaryResponse> classSections
) {
}
