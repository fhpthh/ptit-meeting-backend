package org.ptit.meeting.modules.ai.controller;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.response.AiRealtimeSessionResponse;
import org.ptit.meeting.modules.ai.service.AiRealtimeSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meeting-runtime-sessions/{runtimeSessionId}/ai-realtime")
@RequiredArgsConstructor
public class AiRealtimeSessionController {
  private final AiRealtimeSessionService service;

  @PostMapping("/start")
  public BaseResponse<AiRealtimeSessionResponse> start(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.start(runtimeSessionId, userId));
  }

  @PostMapping("/stop")
  public BaseResponse<AiRealtimeSessionResponse> stop(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.stop(runtimeSessionId, userId));
  }

  @GetMapping
  public BaseResponse<AiRealtimeSessionResponse> status(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.status(runtimeSessionId, userId));
  }
}
