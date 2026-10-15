package org.ptit.meeting.modules.timetable.controller;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.timetable.dto.response.ImportResultResponse;
import org.ptit.meeting.modules.timetable.facade.TimetableAdminFacade;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/timetable")
@RequiredArgsConstructor
public class TimetableAdminController {

  private final TimetableAdminFacade timetableAdminFacade;

  @PostMapping(value = "/master/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<ImportResultResponse> uploadMasterTimetable(
      @RequestParam("semesterId") Long semesterId,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    ImportResultResponse response = timetableAdminFacade.importMasterTimetable(
        semesterId, file, currentUser.getUserId()
    );
    return BaseResponse.success("timetable.import.master.success", response);
  }
}