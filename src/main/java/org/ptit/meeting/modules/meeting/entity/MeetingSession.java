package org.ptit.meeting.modules.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingSessionStatus;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "meeting_sessions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_meeting_sessions_provider_room",
            columnNames = "provider_room_name"
        ),
        @UniqueConstraint(
            name = "uk_meeting_sessions_schedule_occurrence",
            columnNames = "schedule_occurrence_id"
        )
    },
    indexes = {
        @Index(
            name = "idx_meeting_sessions_room_schedule",
            columnList = "meeting_room_id,scheduled_start_at"
        ),
        @Index(
            name = "idx_meeting_sessions_room_status",
            columnList = "meeting_room_id,status"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor(access = PROTECTED)
public class MeetingSession extends BaseEntity {

  @Column(name = "meeting_room_id", nullable = false, length = 36)
  private String meetingRoomId;

  @Column(name = "title", nullable = false, length = 150)
  private String title;

  @Column(name = "provider_room_name", nullable = false, length = 50)
  private String providerRoomName;

  @Column(name = "schedule_occurrence_id", length = 100)
  private String scheduleOccurrenceId;

  @Column(name = "scheduled_start_at", nullable = false)
  private Instant scheduledStartAt;

  @Column(name = "scheduled_end_at", nullable = false)
  private Instant scheduledEndAt;

  @Column(name = "actual_start_at")
  private Instant actualStartAt;

  @Column(name = "actual_end_at")
  private Instant actualEndAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private MeetingSessionStatus status;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long version;

  public MeetingSession(
      String meetingRoomId,
      String title,
      String providerRoomName,
      String scheduleOccurrenceId,
      Instant scheduledStartAt,
      Instant scheduledEndAt,
      Long createdByUserId
  ) {
    this.meetingRoomId = meetingRoomId;
    this.title = title;
    this.providerRoomName = providerRoomName;
    this.scheduleOccurrenceId = scheduleOccurrenceId;
    this.scheduledStartAt = scheduledStartAt;
    this.scheduledEndAt = scheduledEndAt;
    this.status = MeetingSessionStatus.SCHEDULED;
    this.createdByUserId = createdByUserId;
  }
}
