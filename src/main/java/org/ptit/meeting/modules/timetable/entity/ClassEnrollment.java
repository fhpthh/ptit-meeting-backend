package org.ptit.meeting.modules.timetable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;

@Entity
@Table(name = "class_enrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "class_section_id", nullable = false)
  private Long classSectionId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

  @Column(name = "enrolled_at", nullable = false, updatable = false)
  private LocalDateTime enrolledAt;

  @PrePersist
  public void onPrePersist() {
    if (this.enrolledAt == null) {
      this.enrolledAt = LocalDateTime.now();
    }
    if (this.status == null) {
      this.status = EnrollmentStatus.ENROLLED;
    }
  }
}