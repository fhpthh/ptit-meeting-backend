package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionLecturerUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionLecturerResponse;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.timetable.dto.internal.ExportFileResult;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionStudentSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSubjectSearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectFilterResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectSectionsResponse;

public interface SemesterSubjectAdminService {

  List<ClassSectionLecturerResponse> searchLecturers(Long semesterId, String keyword);

  ClassSectionDetailResponse assignLecturer(Long sectionId, ClassSectionLecturerUpdateRequest request);

  PageResponseDTO<SemesterSubjectResponse> search(
      Long semesterId,
      SemesterSubjectSearchRequest request
  );

  SemesterSubjectFilterResponse getFilters(Long semesterId);

  SemesterSubjectDetailResponse getDetail(Long semesterId, Long subjectId, String cohort);

  SemesterSubjectSectionsResponse searchSections(
      Long semesterId,
      Long subjectId,
      ClassSectionSearchRequest request
  );

  ClassSectionDetailResponse getSectionDetail(
      Long semesterId,
      Long subjectId,
      Long sectionId,
      String cohort
  );

  PageResponseDTO<ClassSectionStudentResponse> searchSectionStudents(
      Long semesterId,
      Long subjectId,
      Long sectionId,
      ClassSectionStudentSearchRequest request
  );

  ExportFileResult export(Long semesterId, SemesterSubjectSearchRequest request);
}
