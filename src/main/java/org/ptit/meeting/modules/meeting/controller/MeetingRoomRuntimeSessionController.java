package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRuntimeSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRuntimeSessionResponse;
import org.ptit.meeting.modules.meeting.service.MeetingRuntimeSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting-rooms/{roomId}/runtime-sessions")
@RequiredArgsConstructor
public class MeetingRoomRuntimeSessionController {

  private final MeetingRuntimeSessionService runtimeSessionService;

  @PostMapping
  public BaseResponse<MeetingRuntimeSessionResponse> create(
      @PathVariable String roomId,
      @Valid @RequestBody MeetingRuntimeSessionCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.created(runtimeSessionService.create(roomId, request, currentUserId));
  }

  @GetMapping
  public BaseResponse<List<MeetingRuntimeSessionResponse>> getByRoom(
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(runtimeSessionService.getByRoom(roomId, currentUserId));
  }
}
