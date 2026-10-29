package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.SessionStatus;

public record LecturerOnlineScheduleRow(
    Long sessionId,
    Long scheduleId,
    Long roomId,

    LocalDate studyDate,
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd,
    SessionStatus sessionStatus,

    Integer startPeriod,
    Integer numberOfPeriods,

    Long classSectionId,
    String classCode,
    String groupNumber,
    String practiceGroup,
    String managementClass,

    Long lecturerId,
    String lecturerName,

    String subjectCode,
    String subjectName,

    String roomName
) {
}