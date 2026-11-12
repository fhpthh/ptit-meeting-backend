package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.springframework.stereotype.Component;

@Component
public class MeetingRuntimeSessionMapper {

  public MeetingRuntimeSessionResponse toResponse(MeetingSession runtimeSession) {
    return new MeetingRuntimeSessionResponse(
        runtimeSession.getPublicId(),
        runtimeSession.getMeetingRoomId(),
        runtimeSession.getTitle(),
        runtimeSession.getScheduleOccurrenceId(),
        runtimeSession.getScheduledStartAt(),
        runtimeSession.getScheduledEndAt(),
        runtimeSession.getActualStartAt(),
        runtimeSession.getActualEndAt(),
        runtimeSession.getRuntimeStatus(),
        runtimeSession.getCreatedByUserId(),
        runtimeSession.getCreatedAtInstant(),
        runtimeSession.getUpdatedAtInstant()
    );
  }
}
