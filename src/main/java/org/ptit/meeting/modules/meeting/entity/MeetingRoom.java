package org.ptit.meeting.modules.meeting.entity;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;

@Entity
@Table(
    name = "meeting_rooms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_meeting_rooms_code", columnNames = "code")
    },
    indexes = {
        @Index(name = "idx_meeting_rooms_type_status", columnList = "room_type,status"),
        @Index(name = "idx_meeting_rooms_created_by", columnList = "created_by_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = PROTECTED)
public class MeetingRoom extends BaseEntity {

  @Column(name = "code", nullable = false, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 30)
  private RoomType roomType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private RoomStatus status;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  public MeetingRoom(
      String code,
      String name,
      String description,
      RoomType roomType,
      Long createdByUserId
  ) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.roomType = roomType;
    this.status = RoomStatus.ACTIVE;
    this.createdByUserId = createdByUserId;
  }
}
