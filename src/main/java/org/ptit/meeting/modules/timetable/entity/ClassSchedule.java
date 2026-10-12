package org.ptit.meeting.modules.timetable.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

@Entity
@Table(name = "class_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "class_section_id", nullable = false)
  private Long classSectionId;

  @Column(name = "day_of_week", nullable = false)
  private Integer dayOfWeek;

  @Column(name = "start_period", nullable = false)
  private Integer startPeriod;

  @Column(name = "num_periods", nullable = false)
  private Integer numPeriods;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "weeks_pattern", nullable = false, length = 100)
  private String weeksPattern;

  @Column(name = "room_text", length = 50)
  private String roomText;

  @Column(name = "building", length = 50)
  private String building;

  @Column(name = "is_online", nullable = false)
  @Builder.Default
  private boolean isOnline = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onPrePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (this.createdAt == null) this.createdAt = now;
    if (this.updatedAt == null) this.updatedAt = now;
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}