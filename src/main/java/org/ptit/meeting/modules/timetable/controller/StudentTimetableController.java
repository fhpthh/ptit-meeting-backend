package org.ptit.meeting.modules.timetable.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.service.StudentTimetableService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/timetable")
@RequiredArgsConstructor
public class StudentTimetableController {

  private static final String SEMESTERS_SUCCESS_MESSAGE =
      "timetable.student.semesters.success";

  private static final String WEEKLY_SUCCESS_MESSAGE =
      "timetable.student.weekly.success";

  private final StudentTimetableService studentTimetableService;
  private final MessageUtil messageUtil;

  @GetMapping("/semesters")
  public BaseResponse<List<StudentSemesterResponse>>
  getAvailableSemesters(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    List<StudentSemesterResponse> response =
        studentTimetableService.getAvailableSemesters(
            currentUser.getUserId()
        );

    return BaseResponse.success(
        messageUtil.getMessage(SEMESTERS_SUCCESS_MESSAGE),
        response
    );
  }

  @GetMapping("/weekly")
  public BaseResponse<StudentWeeklyTimetableResponse>
  getWeeklyTimetable(
      @RequestParam Long semesterId,
      @RequestParam int weekNumber,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    StudentWeeklyTimetableResponse response =
        studentTimetableService.getWeeklyTimetable(
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