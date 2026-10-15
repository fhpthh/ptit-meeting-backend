package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultResponse {

  private Long importId;
  private String fileName;
  private String fileSize;
  private Long semesterId;
  private String semesterCode;
  private String semesterName;
  private ImportStatus status;
  private int totalRows;
  private int successRows;
  private int failureRows;
  private ImportStatistics statistics;
  private List<ImportRowError> errors;
  private LocalDateTime completedAt;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ImportStatistics {
    private int headerRow;
    private int dataStartRow;
    private int totalWeeksDetected;
    private int mappedColumnsCount;
    private int subjectsProcessed;
    private int classSectionsProcessed;
    private int classSchedulesSaved;
    private int onlineRoomsAllocated;
    private int meetingSessionsGenerated;
  }
}
