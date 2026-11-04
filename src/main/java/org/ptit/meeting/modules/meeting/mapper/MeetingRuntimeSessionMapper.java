package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRuntimeSession;
import org.springframework.stereotype.Component;

@Component
public class MeetingRuntimeSessionMapper {

  public MeetingRuntimeSessionResponse toResponse(MeetingRuntimeSession runtimeSession) {
    return new MeetingRuntimeSessionResponse(
        runtimeSession.getId(),
        runtimeSession.getMeetingRoomId(),
        runtimeSession.getTitle(),
        runtimeSession.getScheduleOccurrenceId(),
        runtimeSession.getScheduledStartAt(),
        runtimeSession.getScheduledEndAt(),
        runtimeSession.getActualStartAt(),
        runtimeSession.getActualEndAt(),
        runtimeSession.getStatus(),
        runtimeSession.getCreatedByUserId(),
        runtimeSession.getCreatedAt(),
        runtimeSession.getUpdatedAt()
    );
  }
}
