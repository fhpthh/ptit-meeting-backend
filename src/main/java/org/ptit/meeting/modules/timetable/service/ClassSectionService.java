package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSessionResponse;

public interface ClassSectionService {

  List<ClassSectionSemesterResponse> getSemesters(Long userId);

  List<ClassSectionResponse> getOnlineClasses(Long userId, Long semesterId, String academicYear);

  ClassSectionDetailResponse getDetail(Long userId, Long sectionId);

  ClassSessionResponse getSession(Long userId, Long sectionId, Long sessionId);

  PageResponseDTO<ClassSessionResponse> getSessions(
      Long userId, Long sectionId, SessionStatus status, int page, int size
  );

  PageResponseDTO<ClassSectionStudentResponse> getStudents(
      Long userId, Long sectionId, String keyword, String sortBy, int page, int size
  );
}
