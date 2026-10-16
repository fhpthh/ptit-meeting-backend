package org.ptit.meeting.modules.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "meeting_room_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_meeting_room_members_room_user",
            columnNames = {"room_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_meeting_room_members_user", columnList = "user_id"),
        @Index(name = "idx_meeting_room_members_room_status", columnList = "room_id,status")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = PROTECTED)
public class MeetingRoomMember extends BaseEntity {

  @Column(name = "room_id", nullable = false, length = 36)
  private String roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private MeetingRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private MemberStatus status;

  public MeetingRoomMember(String roomId, Long userId, MeetingRole role) {
    this.roomId = roomId;
    this.userId = userId;
    this.role = role;
    this.status = MemberStatus.ACTIVE;
  }
}
