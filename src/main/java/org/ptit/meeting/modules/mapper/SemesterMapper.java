package org.ptit.meeting.modules.mapper;

import org.ptit.meeting.modules.timetable.dto.response.SemesterResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.springframework.stereotype.Component;

@Component
public class SemesterMapper {

  public SemesterResponse toResponse(Semester semester, long totalWeeks) {
    return new SemesterResponse(
        semester.getId(),
        semester.getCode(),
        semester.getName(),
        semester.getAcademicYear(),
        semester.getSemesterNum(),
        semester.getStartDate(),
        semester.getEndDate(),
        semester.isActive(),
        totalWeeks,
        semester.getCreatedAt(),
        semester.getUpdatedAt()
    );
  }
}
