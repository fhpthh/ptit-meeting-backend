package org.ptit.meeting.modules.timetable.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.ptit.meeting.modules.timetable.constant.enums.RoomStatus;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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
    LocalDateTime now = LocalDateTime.now();
    if (this.createdAt == null) this.createdAt = now;
    if (this.updatedAt == null) this.updatedAt = now;
    if (this.status == null) this.status = RoomStatus.ACTIVE;
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}