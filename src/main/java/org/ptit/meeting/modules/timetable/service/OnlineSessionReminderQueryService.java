package org.ptit.meeting.modules.timetable.service;

import java.time.LocalDateTime;
import java.util.List;
import org.ptit.meeting.modules.timetable.dto.internal.UpcomingOnlineSessionDto;

/**
 * Read-only query service exposing upcoming online timetable sessions for reminder scheduling.
 */
public interface OnlineSessionReminderQueryService {

  List<UpcomingOnlineSessionDto> findUpcomingOnlineSessions(
      LocalDateTime windowStart,
      LocalDateTime windowEnd
  );
}
