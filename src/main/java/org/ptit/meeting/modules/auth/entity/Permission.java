
package org.ptit.meeting.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 100)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "module", nullable = false, length = 50)
  private String module;
}
