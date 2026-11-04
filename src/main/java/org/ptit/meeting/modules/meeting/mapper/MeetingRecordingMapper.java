package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRecording;
import org.springframework.stereotype.Component;

@Component
public class MeetingRecordingMapper {

  public MeetingRecordingResponse toResponse(MeetingRecording recording, String url) {
    return new MeetingRecordingResponse(
        recording.getId(),
        recording.getRuntimeSessionId(),
        recording.getMeetingRoomId(),
        recording.getStatus(),
        url,
        recording.getSizeBytes(),
        recording.getDurationMs(),
        recording.getFailureDetail(),
        recording.getRequestedByUserId(),
        recording.getStartedAt(),
        recording.getEndedAt(),
        recording.getCreatedAt(),
        recording.getUpdatedAt()
    );
  }
}
