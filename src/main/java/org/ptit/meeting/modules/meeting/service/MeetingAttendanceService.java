package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.internal.ParticipantConnectionEvent;
import org.ptit.meeting.modules.meeting.dto.response.MeetingAttendanceResponse;

public interface MeetingAttendanceService {

  void recordJoined(ParticipantConnectionEvent event);

  void recordLeft(ParticipantConnectionEvent event);

  void recordAborted(ParticipantConnectionEvent event);

  List<MeetingAttendanceResponse> getAttendance(
      String runtimeSessionId,
      Long currentUserId
  );
}
