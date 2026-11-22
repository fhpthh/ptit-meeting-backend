package org.ptit.meeting.modules.meeting.dto.response;

public record MeetingStudentLookupResponse(
    Long userId,
    String studentCode,
    String fullName,
    String email,
    boolean enrolled
) {
}
