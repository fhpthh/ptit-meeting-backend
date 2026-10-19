package org.ptit.meeting.modules.meeting.controller;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingJoinResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.service.MeetingJoinService;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meeting-runtime-sessions")
@RequiredArgsConstructor
public class MeetingRuntimeSessionController {

  private final MeetingRuntimeSessionService runtimeSessionService;
  private final MeetingJoinService joinService;

  @GetMapping("/{runtimeSessionId}")
  public BaseResponse<MeetingRuntimeSessionResponse> getById(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(runtimeSessionService.getById(runtimeSessionId, currentUserId));
  }

  @PostMapping("/{runtimeSessionId}/start")
  public BaseResponse<MeetingRuntimeSessionResponse> start(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(runtimeSessionService.start(runtimeSessionId, currentUserId));
  }

  @PostMapping("/{runtimeSessionId}/join")
  public BaseResponse<MeetingJoinResponse> join(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(joinService.join(runtimeSessionId, currentUserId));
  }

  @PostMapping("/{runtimeSessionId}/end")
  public BaseResponse<MeetingRuntimeSessionResponse> end(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(runtimeSessionService.end(runtimeSessionId, currentUserId));
  }

  @PostMapping("/{runtimeSessionId}/cancel")
  public BaseResponse<MeetingRuntimeSessionResponse> cancel(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(runtimeSessionService.cancel(runtimeSessionId, currentUserId));
  }
}
