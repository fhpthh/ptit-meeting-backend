package org.ptit.meeting.modules.timetable.facade;

import org.ptit.meeting.modules.timetable.dto.response.ImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface TimetableAdminFacade {
  ImportResultResponse importMasterTimetable(Long semesterId, MultipartFile file, Long uploadedBy);
}