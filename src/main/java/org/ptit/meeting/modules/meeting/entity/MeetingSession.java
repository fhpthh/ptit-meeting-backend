package org.ptit.meeting.modules.meeting.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.*;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRuntimeSessionStatus;

@Entity
@Table(name = "meeting_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sessions_public_id", columnNames = "public_id"),
    @UniqueConstraint(name = "uk_sessions_provider_room", columnNames = "provider_room_name"),
    @UniqueConstraint(name = "uk_sessions_occurrence", columnNames = "schedule_occurrence_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Column(name = "public_id", nullable = false, length = 36)
  private String publicId;

  @Column(name = "title", nullable = false, length = 150)
  @Builder.Default
  private String title = "Online class";

  @Column(name = "provider_room_name", nullable = false, length = 50)
  private String providerRoomName;

  @Column(name = "schedule_occurrence_id", length = 100)
  private String scheduleOccurrenceId;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long version;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", insertable = false, updatable = false)
  private Room room;

  @Column(name = "schedule_id")
  private Long scheduleId;

  @Column(name = "study_date", nullable = false)
  private LocalDate studyDate;

  @Column(name = "scheduled_start", nullable = false)
  private LocalDateTime scheduledStart;

  @Column(name = "scheduled_end", nullable = false)
  private LocalDateTime scheduledEnd;

  @Column(name = "actual_start")
  private LocalDateTime actualStart;

  @Column(name = "actual_end")
  private LocalDateTime actualEnd;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private SessionStatus status = SessionStatus.SCHEDULED;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onPrePersist() {
    if (publicId == null) publicId = UUID.randomUUID().toString();
    if (providerRoomName == null) providerRoomName = "runtime-session-" + publicId.replace("-", "");
    LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
    if (this.createdAt == null) this.createdAt = now;
    if (this.updatedAt == null) this.updatedAt = now;
    if (this.status == null) this.status = SessionStatus.SCHEDULED;
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now(SCHEDULE_ZONE);
  }

  public MeetingSession(Room room, String title, String providerRoomName,
      String scheduleOccurrenceId, Instant scheduledStartAt, Instant scheduledEndAt,
      Long createdByUserId) {
    this.room = room;
    this.roomId = room.getId();
    this.title = title;
    this.providerRoomName = providerRoomName;
    this.scheduleOccurrenceId = scheduleOccurrenceId;
    setScheduledStartAt(scheduledStartAt);
    setScheduledEndAt(scheduledEndAt);
    this.createdByUserId = createdByUserId;
    this.status = SessionStatus.SCHEDULED;
  }

  public String getMeetingRoomId() {
    return room.getPublicId();
  }

  public MeetingRuntimeSessionStatus getRuntimeStatus() {
    return status == SessionStatus.LIVE ? MeetingRuntimeSessionStatus.ACTIVE
        : MeetingRuntimeSessionStatus.valueOf(status.name());
  }

  public void setRuntimeStatus(MeetingRuntimeSessionStatus status) {
    this.status = status == MeetingRuntimeSessionStatus.ACTIVE ? SessionStatus.LIVE
        : SessionStatus.valueOf(status.name());
  }

  public Instant getScheduledStartAt() { return toInstant(scheduledStart); }
  public Instant getScheduledEndAt() { return toInstant(scheduledEnd); }
  public Instant getActualStartAt() { return toInstant(actualStart); }
  public Instant getActualEndAt() { return toInstant(actualEnd); }
  public Instant getCreatedAtInstant() { return toInstant(createdAt); }
  public Instant getUpdatedAtInstant() { return toInstant(updatedAt); }

  public void setScheduledStartAt(Instant value) {
    scheduledStart = toLocalDateTime(value);
    studyDate = scheduledStart == null ? null : scheduledStart.toLocalDate();
  }

  public void setScheduledEndAt(Instant value) { scheduledEnd = toLocalDateTime(value); }
  public void setActualStartAt(Instant value) { actualStart = toLocalDateTime(value); }
  public void setActualEndAt(Instant value) { actualEnd = toLocalDateTime(value); }

  private static Instant toInstant(LocalDateTime value) {
    return value == null ? null : value.atZone(SCHEDULE_ZONE).toInstant();
  }

  private static LocalDateTime toLocalDateTime(Instant value) {
    return value == null ? null : LocalDateTime.ofInstant(value, SCHEDULE_ZONE);
  }
}
