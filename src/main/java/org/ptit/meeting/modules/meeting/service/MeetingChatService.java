package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.request.MeetingChatMessageRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingChatMessageResponse;

public interface MeetingChatService {

  MeetingChatMessageResponse sendMessage(
      String runtimeSessionId,
      MeetingChatMessageRequest request,
      Long currentUserId
  );

  List<MeetingChatMessageResponse> getMessages(
      String runtimeSessionId,
      String before,
      int limit,
      Long currentUserId
  );

  void clearHistory(String runtimeSessionId);
}
