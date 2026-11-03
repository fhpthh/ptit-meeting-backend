package org.ptit.meeting.modules.timetable.service;

import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.timetable.dto.request.SemesterCreateRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterResponse;

public interface SemesterAdminService {

  SemesterResponse create(SemesterCreateRequest request);

  SemesterResponse update(Long id, SemesterUpdateRequest request);

  SemesterResponse activate(Long id);

  SemesterResponse getById(Long id);

  SemesterResponse getActive();

  void delete(Long id);

  PageResponseDTO<SemesterResponse> search(SemesterSearchRequest request);
}
