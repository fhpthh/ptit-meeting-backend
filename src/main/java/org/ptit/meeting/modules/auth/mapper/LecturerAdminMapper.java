package org.ptit.meeting.modules.auth.mapper;

import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;
import org.ptit.meeting.modules.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class LecturerAdminMapper {

  public LecturerAdminResponse toResponse(User user) {
    if (user == null) {
      return null;
    }
    return new LecturerAdminResponse(
        user.getId(),
        user.getCode(),
        user.getFullName(),
        user.getEmail(),
        user.getDepartment(),
        user.getStatus(),
        user.isMustChangePassword(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }
}
