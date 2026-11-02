package org.ptit.meeting.modules.timetable.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.ptit.meeting.common.dto.request.PageRequestDTO;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;

@Getter
@Setter
public class ImportHistorySearchRequest extends PageRequestDTO {

  private Long semesterId;
  private ImportStatus status;

  public ImportHistorySearchRequest() {
    setSortBy("createdAt");
  }
}