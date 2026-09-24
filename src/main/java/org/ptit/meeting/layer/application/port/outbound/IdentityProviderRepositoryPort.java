package org.ptit.meeting.layer.application.port.outbound;

import java.util.Optional;
import org.ptit.meeting.layer.domain.enums.IdpProvider;
import org.ptit.meeting.layer.domain.model.UserIdentityProvider;

public interface IdentityProviderRepositoryPort {

  Optional<UserIdentityProvider> findByProviderAndSubjectId(IdpProvider provider, String subjectId);

  UserIdentityProvider save(UserIdentityProvider userIdentityProvider);
}
