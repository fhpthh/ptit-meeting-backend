package org.ptit.meeting.modules.timetable.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.dto.internal.UpcomingOnlineSessionDto;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.service.OnlineSessionReminderQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnlineSessionReminderQueryServiceImpl implements OnlineSessionReminderQueryService {

  private final MeetingSessionRepository meetingSessionRepository;

  @Override
  @Transactional(readOnly = true)
  public List<UpcomingOnlineSessionDto> findUpcomingOnlineSessions(
      LocalDateTime windowStart,
      LocalDateTime windowEnd
  ) {
    if (windowStart == null || windowEnd == null || !windowEnd.isAfter(windowStart)) {
      return List.of();
    }
    return meetingSessionRepository.findUpcomingOnlineSessions(
        windowStart,
        windowEnd,
        SessionStatus.SCHEDULED,
        ClassSectionStatus.ACTIVE,
        RoomStatus.ACTIVE
    );
  }
}
