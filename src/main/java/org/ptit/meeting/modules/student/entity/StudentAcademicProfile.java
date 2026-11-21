package org.ptit.meeting.modules.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;

@Entity
@Table(name = "student_academic_profiles", uniqueConstraints =
    @UniqueConstraint(name = "uk_student_academic_profiles_user", columnNames = "user_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "administrative_class_id", nullable = false)
  private AdministrativeClass administrativeClass;

  @Enumerated(EnumType.STRING)
  @Column(name = "study_status", nullable = false, length = 30)
  @Builder.Default
  private StudyStatus studyStatus = StudyStatus.ENROLLED;
}
