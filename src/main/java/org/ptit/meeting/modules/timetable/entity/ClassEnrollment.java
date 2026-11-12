package org.ptit.meeting.modules.timetable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;

/** Membership of a class section or a standalone meeting room. */
@Entity
@Table(name = "class_enrollments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_enrollment_class_user", columnNames = {"class_section_id", "user_id"}),
    @UniqueConstraint(name = "uk_enrollment_room_user", columnNames = {"room_id", "user_id"})
}, indexes = {
    @Index(name = "idx_enrollment_user_status", columnList = "user_id,status")
})
@Check(constraints = "(class_section_id is not null and room_id is null and role <> 'OWNER')"
    + " or (class_section_id is null and room_id is not null)")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollment {

  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "class_section_id")
  private Long classSectionId;

  @Column(name = "room_id", length = 36)
  private String roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  @Builder.Default
  private MeetingRole role = MeetingRole.PARTICIPANT;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

  @Column(name = "enrolled_at", nullable = false, updatable = false)
  private LocalDateTime enrolledAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onPrePersist() {
    if (this.enrolledAt == null) {
      this.enrolledAt = LocalDateTime.now(SCHEDULE_ZONE);
    }
    if (this.updatedAt == null) {
      this.updatedAt = this.enrolledAt;
    }
    if (this.status == null) {
      this.status = EnrollmentStatus.ENROLLED;
    }
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now(SCHEDULE_ZONE);
  }
}
