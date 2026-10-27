package org.ptit.meeting.modules.meeting.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.meeting.dto.request.MeetingSessionBanRequest;
import org.ptit.meeting.modules.meeting.dto.request.ParticipantTrackMuteRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanListItemResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanResponse;
import org.ptit.meeting.modules.meeting.service.MeetingModerationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meeting-runtime-sessions/{runtimeSessionId}")
@RequiredArgsConstructor
public class MeetingModerationController {

  private final MeetingModerationService moderationService;

  @PostMapping("/participants/{participantUserId}/mute")
  public BaseResponse<Void> muteTrack(
      @PathVariable String runtimeSessionId,
      @PathVariable Long participantUserId,
      @Valid @RequestBody ParticipantTrackMuteRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    moderationService.muteTrack(
        runtimeSessionId,
        participantUserId,
        request,
        currentUserId
    );
    return BaseResponse.success((Void) null);
  }

  @PostMapping("/participants/{participantUserId}/kick")
  public BaseResponse<Void> kick(
      @PathVariable String runtimeSessionId,
      @PathVariable Long participantUserId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    moderationService.kick(runtimeSessionId, participantUserId, currentUserId);
    return BaseResponse.success((Void) null);
  }

  @PostMapping("/participants/{participantUserId}/ban")
  public BaseResponse<MeetingSessionBanResponse> ban(
      @PathVariable String runtimeSessionId,
      @PathVariable Long participantUserId,
      @Valid @RequestBody MeetingSessionBanRequest request,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        moderationService.ban(runtimeSessionId, participantUserId, request, currentUserId)
    );
  }

  @DeleteMapping("/participants/{participantUserId}/ban")
  public BaseResponse<MeetingSessionBanResponse> unban(
      @PathVariable String runtimeSessionId,
      @PathVariable Long participantUserId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        moderationService.unban(runtimeSessionId, participantUserId, currentUserId)
    );
  }

  @GetMapping("/bans")
  public BaseResponse<List<MeetingSessionBanListItemResponse>> getActiveBans(
      @PathVariable String runtimeSessionId,
      @AuthenticationPrincipal(expression = "userId") Long currentUserId
  ) {
    return BaseResponse.success(
        moderationService.getActiveBans(runtimeSessionId, currentUserId)
    );
  }
}
