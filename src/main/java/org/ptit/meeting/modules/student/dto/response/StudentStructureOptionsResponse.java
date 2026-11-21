package org.ptit.meeting.modules.student.dto.response;

import java.util.List;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;

public record StudentStructureOptionsResponse(
    List<AcademicOptionResponse> campuses,
    List<AcademicOptionResponse> cohorts,
    List<AcademicOptionResponse> majors,
    List<AdministrativeClassOptionResponse> administrativeClasses,
    List<StudyStatus> studyStatuses
) {}
