package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionCreateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionResponse;
import org.ptit.meeting.modules.meeting.service.MeetingSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting-rooms/{roomId}/sessions")
@RequiredArgsConstructor
public class MeetingRoomSessionController {

  private final MeetingSessionService sessionService;

  @PostMapping
  public BaseResponse<MeetingSessionResponse> create(
      @PathVariable String roomId,
      @Valid @RequestBody MeetingSessionCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.created(sessionService.create(roomId, request, currentUserId));
  }

  @GetMapping
  public BaseResponse<List<MeetingSessionResponse>> getByRoom(
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(sessionService.getByRoom(roomId, currentUserId));
  }
}
