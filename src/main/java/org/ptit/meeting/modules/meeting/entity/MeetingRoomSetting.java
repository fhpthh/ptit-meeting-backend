package org.ptit.meeting.modules.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.common.base.BaseEntity;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "meeting_room_settings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_meeting_room_settings_room", columnNames = "room_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = PROTECTED)
public class MeetingRoomSetting extends BaseEntity {

  public static final int MAX_PARTICIPANTS = 100;

  @Column(name = "room_id", nullable = false, length = 36)
  private String roomId;

  @Column(name = "participant_mic_enabled", nullable = false)
  private boolean participantMicEnabled;

  @Column(name = "participant_camera_enabled", nullable = false)
  private boolean participantCameraEnabled;

  @Column(name = "participant_screen_share_enabled", nullable = false)
  private boolean participantScreenShareEnabled;

  @Column(name = "max_participants", nullable = false)
  private int maxParticipants;

  @Column(name = "join_before_minutes", nullable = false)
  private int joinBeforeMinutes;

  public MeetingRoomSetting(
      String roomId,
      boolean participantMicEnabled,
      boolean participantCameraEnabled,
      boolean participantScreenShareEnabled,
      int maxParticipants,
      int joinBeforeMinutes
  ) {
    this.roomId = roomId;
    this.participantMicEnabled = participantMicEnabled;
    this.participantCameraEnabled = participantCameraEnabled;
    this.participantScreenShareEnabled = participantScreenShareEnabled;
    this.maxParticipants = maxParticipants;
    this.joinBeforeMinutes = joinBeforeMinutes;
  }
}
