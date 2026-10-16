package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomSettingUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

  private final MeetingRoomService roomService;

  @PostMapping
  public BaseResponse<MeetingRoomResponse> create(
      @Valid @RequestBody MeetingRoomCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.created(roomService.create(request, currentUserId));
  }

  @GetMapping("/my")
  public BaseResponse<List<MeetingRoomResponse>> getMyRooms(
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(roomService.getMyRooms(currentUserId));
  }

  @GetMapping("/{roomId}")
  public BaseResponse<MeetingRoomResponse> getById(
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(roomService.getById(roomId, currentUserId));
  }

  @PatchMapping("/{roomId}")
  public BaseResponse<MeetingRoomResponse> update(
      @PathVariable String roomId,
      @Valid @RequestBody MeetingRoomUpdateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(roomService.update(roomId, request, currentUserId));
  }

  @PatchMapping("/{roomId}/settings")
  public BaseResponse<MeetingRoomResponse> updateSetting(
      @PathVariable String roomId,
      @Valid @RequestBody MeetingRoomSettingUpdateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(roomService.updateSetting(roomId, request, currentUserId));
  }

  @DeleteMapping("/{roomId}")
  public BaseResponse<Void> archive(
      @PathVariable String roomId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    roomService.archive(roomId, currentUserId);
    return BaseResponse.success((Void) null);
  }
}
