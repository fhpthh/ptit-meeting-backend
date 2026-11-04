package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberRoleUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;
import org.ptit.meeting.modules.meeting.service.MeetingRoomMemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting-rooms/{roomId}/members")
@RequiredArgsConstructor
public class MeetingRoomMemberController {

  private final MeetingRoomMemberService memberService;

  @PostMapping
  public BaseResponse<MeetingRoomMemberResponse> add(
      @PathVariable String roomId,
      @Valid @RequestBody MeetingRoomMemberCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.created(memberService.add(roomId, request, currentUserId));
  }

  @GetMapping
  public BaseResponse<List<MeetingRoomMemberResponse>> getActiveMembers(
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(memberService.getActiveMembers(roomId, currentUserId));
  }

  @PatchMapping("/{memberUserId}/role")
  public BaseResponse<MeetingRoomMemberResponse> updateRole(
      @PathVariable Long memberUserId,
      @PathVariable String roomId,
      @Valid @RequestBody MeetingRoomMemberRoleUpdateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        memberService.updateRole(roomId, memberUserId, request, currentUserId)
    );
  }

  @DeleteMapping("/{memberUserId}")
  public BaseResponse<Void> remove(
      @PathVariable Long memberUserId,
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    memberService.remove(roomId, memberUserId, currentUserId);
    return BaseResponse.success((Void) null);
  }
}
