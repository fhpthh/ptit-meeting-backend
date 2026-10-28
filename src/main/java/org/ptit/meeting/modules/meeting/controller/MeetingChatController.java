package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingChatMessageRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingChatMessageResponse;
import org.ptit.meeting.modules.meeting.service.MeetingChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meeting-runtime-sessions/{runtimeSessionId}/chat/messages")
@RequiredArgsConstructor
public class MeetingChatController {

  private final MeetingChatService chatService;

  @PostMapping
  public BaseResponse<MeetingChatMessageResponse> sendMessage(
      @PathVariable String runtimeSessionId,
      @Valid @RequestBody MeetingChatMessageRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        chatService.sendMessage(runtimeSessionId, request, currentUserId)
    );
  }

  @GetMapping
  public BaseResponse<List<MeetingChatMessageResponse>> getMessages(
      @PathVariable String runtimeSessionId,
      @RequestParam(required = false) String before,
      @RequestParam(defaultValue = "50") int limit,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        chatService.getMessages(runtimeSessionId, before, limit, currentUserId)
    );
  }
}
