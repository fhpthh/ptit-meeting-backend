
package org.ptit.meeting.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 150)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "code", length = 30)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false)
  private AccountType accountType;

  @Column(name = "department", length = 150)
  private String department;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "must_change_password", nullable = false)
  @Builder.Default
  private boolean mustChangePassword = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Transient
  @Builder.Default
  private Set<String> roles = new HashSet<>();

  @Transient
  @Builder.Default
  private Set<String> permissions = new HashSet<>();

  @PrePersist
  public void onPrePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = now;
    }
    if (this.status == null) {
      this.status = UserStatus.ACTIVE;
    }
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isActive() {
    return UserStatus.ACTIVE.equals(this.status);
  }
}
