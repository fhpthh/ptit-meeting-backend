package org.ptit.meeting.service;

import java.util.Optional;
import org.ptit.meeting.constant.enums.IdpProvider;
import org.ptit.meeting.entity.UserIdentityProvider;

public interface IdentityProviderService {

  Optional<Long> findUserIdByProviderAndSubject(IdpProvider provider, String subjectId);

  void updateLastLogin(IdpProvider provider, String subjectId);

  UserIdentityProvider createLink(Long userId, IdpProvider provider, String subjectId, String email);
}
