package org.ptit.meeting.modules.meeting.mapper;

import java.time.ZoneId;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomMemberMapper {

  public MeetingRoomMemberResponse toResponse(ClassEnrollment member, String roomId) {
    return new MeetingRoomMemberResponse(
        member.getId() == null ? null : member.getId().toString(),
        roomId,
        member.getUserId(),
        member.getRole(),
        member.getStatus() == EnrollmentStatus.ENROLLED ? MemberStatus.ACTIVE : MemberStatus.INACTIVE,
        member.getEnrolledAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
        member.getUpdatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
    );
  }
}
