package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.ptit.meeting.common.dto.request.PageRequestDTO;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;

@Getter
@Setter
public class LecturerAdminSearchRequest extends PageRequestDTO {
  @Size(max = 150)
  private String keyword;

  @Size(max = 150)
  private String department;

  private UserStatus status;
}
