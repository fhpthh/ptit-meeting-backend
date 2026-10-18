package org.ptit.meeting.modules.meeting.controller;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionResponse;
import org.ptit.meeting.modules.meeting.service.MeetingSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meeting-sessions")
@RequiredArgsConstructor
public class MeetingSessionController {

  private final MeetingSessionService sessionService;

  @GetMapping("/{sessionId}")
  public BaseResponse<MeetingSessionResponse> getById(
      @PathVariable String sessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(sessionService.getById(sessionId, currentUserId));
  }

  @PostMapping("/{sessionId}/start")
  public BaseResponse<MeetingSessionResponse> start(
      @PathVariable String sessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(sessionService.start(sessionId, currentUserId));
  }

  @PostMapping("/{sessionId}/end")
  public BaseResponse<MeetingSessionResponse> end(
      @PathVariable String sessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(sessionService.end(sessionId, currentUserId));
  }

  @PostMapping("/{sessionId}/cancel")
  public BaseResponse<MeetingSessionResponse> cancel(
      @PathVariable String sessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(sessionService.cancel(sessionId, currentUserId));
  }
}
