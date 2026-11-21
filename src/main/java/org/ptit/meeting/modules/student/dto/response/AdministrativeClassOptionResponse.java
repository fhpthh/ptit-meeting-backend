package org.ptit.meeting.modules.student.dto.response;

public record AdministrativeClassOptionResponse(
    Long id, String code, String name, Long campusId, Long cohortId, Long majorId
) {}
