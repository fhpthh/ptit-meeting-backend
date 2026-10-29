package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDateTime;

public record LecturerScheduleTimeRow(
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd
) {
}