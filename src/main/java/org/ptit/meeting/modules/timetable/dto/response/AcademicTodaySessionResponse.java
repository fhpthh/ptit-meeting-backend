package org.ptit.meeting.modules.timetable.dto.response;

public record AcademicTodaySessionResponse(
    Long sessionId,
    String shift,
    String courseName,
    String courseCode,
    String sectionCode,
    String lecturerName,
    String lecturerCode,
    String roomName,
    String pin,
    String status
) {
}
