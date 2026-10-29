package org.ptit.meeting.modules.timetable.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.timetable.dto.response.LecturerSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.LecturerWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.service.LecturerTimetableService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lecturer/timetable")
@RequiredArgsConstructor
public class LecturerTimetableController {

  private static final String SEMESTERS_SUCCESS_MESSAGE ="timetable.lecturer.semesters.success";

  private static final String WEEKLY_SUCCESS_MESSAGE ="timetable.lecturer.weekly.success";

  private final LecturerTimetableService lecturerTimetableService;
  private final MessageUtil messageUtil;

  @GetMapping("/semesters")
  public BaseResponse<List<LecturerSemesterResponse>>
  getAvailableSemesters(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    List<LecturerSemesterResponse> response =
        lecturerTimetableService.getAvailableSemesters(
            currentUser.getUserId()
        );

    return BaseResponse.success(
        messageUtil.getMessage(SEMESTERS_SUCCESS_MESSAGE),
        response
    );
  }

  @GetMapping("/weekly")
  public BaseResponse<LecturerWeeklyTimetableResponse>
  getWeeklyTimetable(
      @RequestParam Long semesterId,
      @RequestParam int weekNumber,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    LecturerWeeklyTimetableResponse response =
        lecturerTimetableService.getWeeklyTimetable(
            currentUser.getUserId(),
            semesterId,
            weekNumber
        );

    return BaseResponse.success(
        messageUtil.getMessage(WEEKLY_SUCCESS_MESSAGE),
        response
    );
  }
}