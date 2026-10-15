package org.ptit.meeting.modules.timetable.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowError {
  private int rowNumber;
  private String errorMessage;
}
