package org.ptit.meeting.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.request.NotificationPreferenceUpdateRequest;
import org.ptit.meeting.modules.notification.dto.response.MarkAllReadResponse;
import org.ptit.meeting.modules.notification.dto.response.NotificationPreferenceResponse;
import org.ptit.meeting.modules.notification.dto.response.UnreadCountResponse;
import org.ptit.meeting.modules.notification.dto.response.UserNotificationResponse;
import org.ptit.meeting.modules.notification.service.NotificationInboxService;
import org.ptit.meeting.modules.notification.service.NotificationPreferenceService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "User Notifications", description = "Personal notification inbox and preferences")
public class NotificationController {

  private static final String MSG_READ_SUCCESS = "notification.read.success";

  private final NotificationInboxService inboxService;
  private final NotificationPreferenceService preferenceService;
  private final MessageUtil messageUtil;

  @GetMapping
  @Operation(summary = "Get current user's notification inbox")
  public BaseResponse<PageResponseDTO<UserNotificationResponse>> getInbox(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @RequestParam(required = false) NotificationType type,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) Boolean read,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return BaseResponse.success(
        inboxService.getUserInbox(currentUser.getUserId(), type, category, read, page, size)
    );
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Get unread notification count for current user")
  public BaseResponse<UnreadCountResponse> getUnreadCount(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(inboxService.getUnreadCount(currentUser.getUserId()));
  }

  @PatchMapping("/{recipientId}/read")
  @Operation(summary = "Mark a notification item as read")
  public BaseResponse<Void> markAsRead(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @PathVariable String recipientId
  ) {
    inboxService.markAsRead(currentUser.getUserId(), recipientId);
    return BaseResponse.success(messageUtil.getMessage(MSG_READ_SUCCESS));
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark all unread notifications as read")
  public BaseResponse<MarkAllReadResponse> markAllAsRead(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(inboxService.markAllAsRead(currentUser.getUserId()));
  }

  @GetMapping("/preferences")
  @Operation(summary = "Get notification preferences for current user")
  public BaseResponse<NotificationPreferenceResponse> getPreferences(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(preferenceService.getPreferences(currentUser.getUserId()));
  }

  @PutMapping("/preferences")
  @Operation(summary = "Update notification preferences for current user")
  public BaseResponse<NotificationPreferenceResponse> updatePreferences(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @Valid @RequestBody NotificationPreferenceUpdateRequest request
  ) {
    return BaseResponse.success(
        preferenceService.updatePreferences(currentUser.getUserId(), request)
    );
  }
}
