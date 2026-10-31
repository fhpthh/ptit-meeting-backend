package org.ptit.meeting.modules.meeting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingHandRaiseResponse;
import org.ptit.meeting.modules.meeting.service.MeetingHandRaiseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meeting-runtime-sessions/{runtimeSessionId}/hand-raises")
@RequiredArgsConstructor
public class MeetingHandRaiseController {

  private final MeetingHandRaiseService handRaiseService;

  @PutMapping("/me")
  public BaseResponse<MeetingHandRaiseResponse> raise(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(handRaiseService.raise(runtimeSessionId, currentUserId));
  }

  @DeleteMapping("/me")
  public BaseResponse<Void> lower(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    handRaiseService.lower(runtimeSessionId, currentUserId);
    return BaseResponse.success((Void) null);
  }

  @GetMapping
  public BaseResponse<List<MeetingHandRaiseResponse>> getRaisedHands(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        handRaiseService.getRaisedHands(runtimeSessionId, currentUserId)
    );
  }
}
