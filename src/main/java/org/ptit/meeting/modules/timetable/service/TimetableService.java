package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.internal.MasterTimetableRowDto;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

public interface TimetableService {

  int processMasterTimetable(Long semesterId, List<MasterTimetableRowDto> rows,
      List<ImportRowError> errors);
}