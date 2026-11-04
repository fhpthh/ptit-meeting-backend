package org.ptit.meeting.modules.meeting.mapper;

import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanListItemResponse;
import org.ptit.meeting.modules.meeting.dto.response.MeetingSessionBanResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingSessionBan;
import org.springframework.stereotype.Component;

@Component
public class MeetingSessionBanMapper {

  public MeetingSessionBanResponse toResponse(MeetingSessionBan entity) {
    return new MeetingSessionBanResponse(
        entity.getRuntimeSessionId(),
        entity.getUserId(),
        entity.getBannedByUserId(),
        entity.getReason(),
        entity.getBannedAt(),
        entity.isActive(),
        entity.getRevokedAt(),
        entity.getRevokedByUserId()
    );
  }

  public MeetingSessionBanListItemResponse toListItem(
      MeetingSessionBan entity,
      User user
  ) {
    return new MeetingSessionBanListItemResponse(
        entity.getId(),
        user.getId(),
        user.getEmail(),
        user.getCode(),
        user.getFullName(),
        user.getAccountType(),
        user.getDepartment(),
        entity.getBannedByUserId(),
        entity.getReason(),
        entity.getBannedAt()
    );
  }
}
