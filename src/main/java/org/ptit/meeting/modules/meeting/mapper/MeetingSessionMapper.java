package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.springframework.stereotype.Component;

@Component
public class MeetingSessionMapper {

  public MeetingSessionResponse toResponse(MeetingSession session) {
    return new MeetingSessionResponse(
        session.getId(),
        session.getMeetingRoomId(),
        session.getTitle(),
        session.getScheduleOccurrenceId(),
        session.getScheduledStartAt(),
        session.getScheduledEndAt(),
        session.getActualStartAt(),
        session.getActualEndAt(),
        session.getStatus(),
        session.getCreatedByUserId(),
        session.getCreatedAt(),
        session.getUpdatedAt()
    );
  }
}
