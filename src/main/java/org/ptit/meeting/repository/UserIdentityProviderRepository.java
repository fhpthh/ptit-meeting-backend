package org.ptit.meeting.repository;

import java.util.Optional;
import org.ptit.meeting.constant.enums.IdpProvider;
import org.ptit.meeting.entity.UserIdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIdentityProviderRepository extends JpaRepository<UserIdentityProvider, Long> {

  Optional<UserIdentityProvider> findByProviderAndProviderSubjectId(
      IdpProvider provider,
      String providerSubjectId
  );

  Optional<UserIdentityProvider> findByUserIdAndProvider(
      Long userId,
      IdpProvider provider
  );
}
