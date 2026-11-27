package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpcomingOnlineSessionDto(
    Long sessionId,
    String sessionPublicId,
    Long scheduleId,
    Long classSectionId,
    String classCode,
    String groupNumber,
    String subjectCode,
    String subjectName,
    Long lecturerId,
    String lecturerName,
    String roomName,
    String roomPublicId,
    LocalDate studyDate,
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd
) {
}
