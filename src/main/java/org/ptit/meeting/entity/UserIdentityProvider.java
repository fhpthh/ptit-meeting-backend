package org.ptit.meeting.entity;

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
import org.ptit.meeting.constant.enums.IdpProvider;

@Entity
@Table(name = "user_identity_providers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityProvider {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 50)
  private IdpProvider provider;

  @Column(name = "provider_subject_id", nullable = false, length = 100)
  private String providerSubjectId;

  @Column(name = "email_at_login", nullable = false, length = 150)
  private String emailAtLogin;

  @Column(name = "linked_at", nullable = false, updatable = false)
  private LocalDateTime linkedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @PrePersist
  public void onPrePersist() {
    if (this.linkedAt == null) {
      this.linkedAt = LocalDateTime.now();
    }
  }
}
