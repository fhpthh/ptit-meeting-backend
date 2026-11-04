package org.ptit.meeting.modules.meeting.service;

import java.util.List;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCompleteRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingUploadPartResponse;

public interface MeetingRecordingService {

  MeetingRecordingResponse start(
      String runtimeSessionId,
      MeetingRecordingCreateRequest request,
      Long currentUserId
  );

  MeetingRecordingUploadPartResponse createUploadPartUrl(
      String recordingId,
      int partNumber,
      Long currentUserId
  );

  MeetingRecordingResponse complete(
      String recordingId,
      MeetingRecordingCompleteRequest request,
      Long currentUserId
  );

  MeetingRecordingResponse abort(String recordingId, Long currentUserId);

  List<MeetingRecordingResponse> getByRuntimeSession(
      String runtimeSessionId,
      Long currentUserId
  );

  MeetingRecordingResponse getById(String recordingId, Long currentUserId);

  void cleanupStaleUploads();
}
