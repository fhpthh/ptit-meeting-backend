package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDashboardKpiResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDepartmentStatResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicTodaySessionResponse;

public interface AcademicDashboardService {

  AcademicDashboardKpiResponse getKpi();

  List<AcademicTodaySessionResponse> getTodaySessions();

  List<AcademicDepartmentStatResponse> getDepartmentStats();
}
