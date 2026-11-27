package org.ptit.meeting.modules.auth.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LecturerImportResponse {

  private int totalRows;
  private int successCount;
  private int failureCount;
  private List<RowError> errors;

  public record RowError(int rowNumber, String lecturerCode, String errorMessage) {

  }
}