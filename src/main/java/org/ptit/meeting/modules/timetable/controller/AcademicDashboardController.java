package org.ptit.meeting.modules.timetable.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDashboardKpiResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDepartmentStatResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicTodaySessionResponse;
import org.ptit.meeting.modules.timetable.service.AcademicDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AcademicDashboardController {

  private final AcademicDashboardService academicDashboardService;

  @GetMapping("/kpi")
  public BaseResponse<AcademicDashboardKpiResponse> getKpi() {
    return BaseResponse.success(academicDashboardService.getKpi());
  }

  @GetMapping("/today-sessions")
  public BaseResponse<List<AcademicTodaySessionResponse>> getTodaySessions() {
    return BaseResponse.success(academicDashboardService.getTodaySessions());
  }

  @GetMapping("/department-stats")
  public BaseResponse<List<AcademicDepartmentStatResponse>> getDepartmentStats() {
    return BaseResponse.success(academicDashboardService.getDepartmentStats());
  }
}
