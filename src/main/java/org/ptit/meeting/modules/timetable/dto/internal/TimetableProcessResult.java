package org.ptit.meeting.modules.timetable.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableProcessResult {

  private int successCount;
  private int subjectsProcessed;
  private int classSectionsProcessed;
  private int classSchedulesSaved;
  private int onlineRoomsAllocated;
  private int meetingSessionsGenerated;
}
