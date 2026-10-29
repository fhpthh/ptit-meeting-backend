package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDateTime;

public record StudentScheduleTimeRow(
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd
) {
}
