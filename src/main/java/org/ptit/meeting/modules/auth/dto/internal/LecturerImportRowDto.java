package org.ptit.meeting.modules.auth.dto.internal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LecturerImportRowDto {

  private Integer stt;
  private String fullName;
  private String lecturerCode;
  private String email;
  private String department;
}