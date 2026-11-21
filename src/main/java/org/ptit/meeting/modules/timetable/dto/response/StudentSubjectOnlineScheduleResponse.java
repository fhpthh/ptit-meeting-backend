package org.ptit.meeting.modules.timetable.dto.response;

import java.util.List;

public record StudentSubjectOnlineScheduleResponse(
    StudentSemesterResponse semester,
    StudentOnlineSubjectResponse subject,
    List<StudentWeeklyTimetableResponse.TimetableItem> items
) {
}
