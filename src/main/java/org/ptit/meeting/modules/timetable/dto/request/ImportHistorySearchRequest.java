package org.ptit.meeting.modules.timetable.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.ptit.meeting.common.dto.request.PageRequestDTO;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ImportHistorySearchRequest extends PageRequestDTO {

  private Long semesterId;
  private ImportStatus status;
  private ImportType importType;
  private Long uploadedBy;
  private String keyword;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fromDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate toDate;

  public ImportHistorySearchRequest() {
    setSortBy("createdAt");
  }
}
