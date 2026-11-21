package org.ptit.meeting.modules.timetable.service;

import java.util.Optional;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSubjectResponse;

public interface ClassSectionSubjectService {
  Optional<ClassSectionSubjectResponse> findSubject(Long classSectionId);
}
