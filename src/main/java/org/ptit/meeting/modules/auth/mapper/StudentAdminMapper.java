package org.ptit.meeting.modules.auth.mapper;

import org.ptit.meeting.modules.auth.dto.response.StudentAdminResponse;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentAdminMapper {
  public StudentAdminResponse toResponse(User user) {
    return toResponse(user, null);
  }

  public StudentAdminResponse toResponse(User user, StudentAcademicProfileResponse academicProfile) {
    return new StudentAdminResponse(user.getId(), user.getCode(), user.getFullName(),
        user.getEmail(), user.getDepartment(), user.getStatus(), user.isMustChangePassword(),
        user.getCreatedAt(), user.getUpdatedAt(), academicProfile);
  }
}
