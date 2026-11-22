package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;

public record ClassSessionResponse(
    Long id,
    String runtimeSessionId,
    String title,
    LocalDate studyDate,
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd,
    SessionStatus status
) {
}
