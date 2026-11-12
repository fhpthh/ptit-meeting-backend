package org.ptit.meeting.modules.meeting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;

@Entity
@Table(name = "rooms", uniqueConstraints = {
    @UniqueConstraint(name = "uk_rooms_public_id", columnNames = "public_id"),
    @UniqueConstraint(name = "uk_rooms_code", columnNames = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, length = 36)
  private String publicId;

  @Column(name = "code", nullable = false, length = 50)
  private String code;

  @Column(name = "description", length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 30)
  @Builder.Default
  private RoomType roomType = RoomType.CLASS;

  @Column(name = "class_section_id", unique = true)
  private Long classSectionId;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "external_engine_id", length = 100)
  private String externalEngineId;

  @Column(name = "passcode", length = 20)
  private String passcode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private RoomStatus status = RoomStatus.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onPrePersist() {
    if (publicId == null) publicId = UUID.randomUUID().toString();
    if (code == null) code = "ROOM-" + publicId.replace("-", "").toUpperCase(java.util.Locale.ROOT);
    LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
    if (this.createdAt == null) this.createdAt = now;
    if (this.updatedAt == null) this.updatedAt = now;
    if (this.status == null) this.status = RoomStatus.ACTIVE;
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now(SCHEDULE_ZONE);
  }

  public Room(String code, String name, String description, RoomType roomType, Long createdByUserId) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.roomType = roomType;
    this.createdBy = createdByUserId;
    this.status = RoomStatus.ACTIVE;
  }

  public Long getCreatedByUserId() {
    return createdBy;
  }

  public Instant getCreatedAtInstant() {
    return createdAt == null ? null : createdAt.atZone(SCHEDULE_ZONE).toInstant();
  }

  public Instant getUpdatedAtInstant() {
    return updatedAt == null ? null : updatedAt.atZone(SCHEDULE_ZONE).toInstant();
  }
}
