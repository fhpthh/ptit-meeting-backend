package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDate;

public record AcademicDashboardKpiResponse(
    Long semesterId,
    String semesterName,
    String academicYear,
    Integer weekCurrent,
    Integer weekTotal,
    LocalDate startDate,
    LocalDate endDate,
    long lecturerTotal,
    long lecturerActive,
    long lecturerInactive,
    long classSectionTotal,
    long subjectTotal,
    long onlineRoomTotal,
    long meetingSessionTotal
) {
}
