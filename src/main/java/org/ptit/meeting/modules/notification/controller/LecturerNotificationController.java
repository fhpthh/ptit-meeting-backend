package org.ptit.meeting.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.notification.constant.enums.NotificationCategory;
import org.ptit.meeting.modules.notification.constant.enums.NotificationPriority;
import org.ptit.meeting.modules.notification.dto.request.LecturerAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.service.LecturerNotificationService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lecturer/notifications")
@RequiredArgsConstructor
@Tag(name = "Lecturer Notifications", description = "Lecturer class announcement management")
public class LecturerNotificationController {

  private static final String MSG_DELETED = "notification.deleted.success";

  private final LecturerNotificationService lecturerNotificationService;
  private final MessageUtil messageUtil;

  @PostMapping
  @Operation(summary = "Send an announcement to students of lecturer-owned class sections")
  public BaseResponse<AnnouncementResponse> createAnnouncement(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @Valid @RequestBody LecturerAnnouncementCreateRequest request
  ) {
    return BaseResponse.created(
        lecturerNotificationService.createAnnouncement(currentUser.getUserId(), request)
    );
  }

  @GetMapping
  @Operation(summary = "List announcements sent by the authenticated lecturer")
  public BaseResponse<PageResponseDTO<AnnouncementResponse>> listOwnAnnouncements(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) NotificationPriority priority,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return BaseResponse.success(
        lecturerNotificationService.listOwnAnnouncements(
            currentUser.getUserId(), category, priority, keyword, page, size
        )
    );
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get detail and read statistics of an announcement sent by the lecturer")
  public BaseResponse<AnnouncementResponse> getOwnAnnouncementDetail(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @PathVariable String id
  ) {
    return BaseResponse.success(
        lecturerNotificationService.getOwnAnnouncementDetail(currentUser.getUserId(), id)
    );
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke/delete an announcement sent by the lecturer")
  public BaseResponse<Void> deleteOwnAnnouncement(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @PathVariable String id
  ) {
    lecturerNotificationService.deleteOwnAnnouncement(currentUser.getUserId(), id);
    return BaseResponse.success(messageUtil.getMessage(MSG_DELETED));
  }
}
