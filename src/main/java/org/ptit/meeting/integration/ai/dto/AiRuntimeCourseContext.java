package org.ptit.meeting.integration.ai.dto;

public record AiRuntimeCourseContext(
    String subjectCode,
    String subjectName,
    String terminologyProfileId,
    int terminologyProfileVersion
) {
}
