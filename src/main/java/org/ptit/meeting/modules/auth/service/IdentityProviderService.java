
package org.ptit.meeting.modules.auth.service;

import java.util.Optional;
import org.ptit.meeting.modules.auth.constant.enums.IdpProvider;
import org.ptit.meeting.modules.auth.entity.UserIdentityProvider;

public interface IdentityProviderService {

  Optional<Long> findUserIdByProviderAndSubject(IdpProvider provider, String subjectId);

  void updateLastLogin(IdpProvider provider, String subjectId);

  UserIdentityProvider createLink(Long userId, IdpProvider provider, String subjectId, String email);
}
