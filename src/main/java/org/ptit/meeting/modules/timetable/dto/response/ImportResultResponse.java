package org.ptit.meeting.modules.timetable.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultResponse {

  private int totalRows;
  private int successRows;
  private int failureRows;
  private List<ImportRowError> errors;
}
