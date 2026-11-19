package org.ptit.meeting.modules.student.dto.response;

import org.ptit.meeting.modules.student.constant.enums.StudyStatus;

public record StudentAcademicProfileResponse(
    Long id,
    StudyStatus studyStatus,
    AcademicOptionResponse campus,
    AcademicOptionResponse cohort,
    AcademicOptionResponse major,
    AcademicOptionResponse administrativeClass
) {}
