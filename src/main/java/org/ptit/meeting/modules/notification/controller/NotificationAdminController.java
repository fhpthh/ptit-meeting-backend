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
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.dto.request.AdminAnnouncementCreateRequest;
import org.ptit.meeting.modules.notification.dto.response.AdminAudienceOptionsResponse;
import org.ptit.meeting.modules.notification.dto.response.AnnouncementResponse;
import org.ptit.meeting.modules.notification.service.NotificationAdminService;
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
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "Academic Affairs Office (PGV) notification management")
public class NotificationAdminController {

  private static final String MSG_DELETED = "notification.deleted.success";

  private final NotificationAdminService notificationAdminService;
  private final MessageUtil messageUtil;

  @PostMapping
  @Operation(summary = "Create and publish an announcement from Academic Affairs Office (PGV)")
  public BaseResponse<AnnouncementResponse> createAnnouncement(
      @AuthenticationPrincipal CustomUserDetails currentUser,
      @Valid @RequestBody AdminAnnouncementCreateRequest request
  ) {
    return BaseResponse.created(
        notificationAdminService.createAnnouncement(currentUser.getUserId(), request)
    );
  }

  @GetMapping
  @Operation(summary = "List sent announcements with recipient and read statistics")
  public BaseResponse<PageResponseDTO<AnnouncementResponse>> listAnnouncements(
      @RequestParam(required = false) NotificationType type,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) NotificationPriority priority,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return BaseResponse.success(
        notificationAdminService.listAnnouncements(type, category, priority, keyword, page, size)
    );
  }

  @GetMapping("/audience-options")
  @Operation(summary = "List active departments and class sections for announcement targeting")
  public BaseResponse<AdminAudienceOptionsResponse> getAudienceOptions(
      @RequestParam(required = false) String keyword
  ) {
    return BaseResponse.success(notificationAdminService.getAudienceOptions(keyword));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get announcement detail and read statistics")
  public BaseResponse<AnnouncementResponse> getAnnouncementDetail(@PathVariable String id) {
    return BaseResponse.success(notificationAdminService.getAnnouncementDetail(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke/delete an announcement")
  public BaseResponse<Void> deleteAnnouncement(@PathVariable String id) {
    notificationAdminService.deleteAnnouncement(id);
    return BaseResponse.success(messageUtil.getMessage(MSG_DELETED));
  }
}
