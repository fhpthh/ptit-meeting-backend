package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCompleteRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRecordingCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRecordingUploadPartResponse;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeetingRecordingController {

  private final MeetingRecordingService recordingService;

  @PostMapping("/meeting-runtime-sessions/{runtimeSessionId}/recordings")
  public BaseResponse<MeetingRecordingResponse> start(
      @PathVariable String runtimeSessionId,
      @Valid @RequestBody MeetingRecordingCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.created(
        recordingService.start(runtimeSessionId, request, currentUserId)
    );
  }

  @GetMapping("/meeting-runtime-sessions/{runtimeSessionId}/recordings")
  public BaseResponse<List<MeetingRecordingResponse>> getByRuntimeSession(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        recordingService.getByRuntimeSession(runtimeSessionId, currentUserId)
    );
  }

  @GetMapping("/meeting-recordings/{recordingId}")
  public BaseResponse<MeetingRecordingResponse> getById(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(recordingService.getById(recordingId, currentUserId));
  }

  @PostMapping("/meeting-recordings/{recordingId}/upload-parts/{partNumber}/url")
  public BaseResponse<MeetingRecordingUploadPartResponse> createUploadPartUrl(
      @PathVariable String recordingId,
      @PathVariable int partNumber,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        recordingService.createUploadPartUrl(recordingId, partNumber, currentUserId)
    );
  }

  @PostMapping("/meeting-recordings/{recordingId}/complete")
  public BaseResponse<MeetingRecordingResponse> complete(
      @PathVariable String recordingId,
      @Valid @RequestBody MeetingRecordingCompleteRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        recordingService.complete(recordingId, request, currentUserId)
    );
  }

  @PostMapping("/meeting-recordings/{recordingId}/abort")
  public BaseResponse<MeetingRecordingResponse> abort(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(recordingService.abort(recordingId, currentUserId));
  }
}
