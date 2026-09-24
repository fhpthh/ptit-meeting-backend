package org.ptit.meeting.layer.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.layer.domain.enums.IdpProvider;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdentityProvider {

  private Long id;
  private Long userId;
  private IdpProvider idpProvider;
  private String providerSubjectId;
  private String emailAtLogin;
  private LocalDateTime linkedAt;
  private LocalDateTime lastLogin;
}
