package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.ptit.meeting.common.dto.request.PageRequestDTO;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;

@Getter
@Setter
public class StudentAdminSearchRequest extends PageRequestDTO {
  @Size(max = 150, message = "{validation.student-admin.text.size}")
  private String keyword;
  @Size(max = 150, message = "{validation.student-admin.text.size}")
  private String department;
  private UserStatus status;
  private Long campusId;
  private Long cohortId;
  private Long majorId;
  private Long administrativeClassId;
  private StudyStatus studyStatus;
}
