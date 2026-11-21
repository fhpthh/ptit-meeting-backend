package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentAdminEnrollmentSearchRequest {
  @Positive(message = "{validation.student-admin.id.positive}")
  private Long semesterId;

  @Min(value = 0, message = "{validation.page.number.min}")
  private int page = 0;

  @Min(value = 1, message = "{validation.page.size.min}")
  @Max(value = 100, message = "{validation.page.size.max}")
  private int size = 20;
}
