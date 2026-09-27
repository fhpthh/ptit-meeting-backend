
package org.ptit.meeting.modules.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.constant.enums.IdpProvider;
import org.ptit.meeting.modules.auth.entity.UserIdentityProvider;
import org.ptit.meeting.modules.auth.repository.UserIdentityProviderRepository;
import org.ptit.meeting.modules.auth.service.IdentityProviderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityProviderServiceImpl implements IdentityProviderService {

  private final UserIdentityProviderRepository identityProviderRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<Long> findUserIdByProviderAndSubject(IdpProvider provider, String subjectId) {
    log.info("(findUserIdByProviderAndSubject) provider: {}, subjectId: {}", provider, subjectId);
    return identityProviderRepository.findByProviderAndProviderSubjectId(provider, subjectId)
        .map(UserIdentityProvider::getUserId);
  }

  @Override
  @Transactional
  public void updateLastLogin(IdpProvider provider, String subjectId) {
    log.info("(updateLastLogin) provider: {}, subjectId: {}", provider, subjectId);
    identityProviderRepository.findByProviderAndProviderSubjectId(provider, subjectId)
        .ifPresent(idp -> {
          idp.setLastLoginAt(LocalDateTime.now());
          identityProviderRepository.save(idp);
        });
  }

  @Override
  @Transactional
  public UserIdentityProvider createLink(Long userId, IdpProvider provider, String subjectId, String email) {
    log.info("(createLink) Linking userId: {} with provider: {}, subjectId: {}", userId, provider, subjectId);
    UserIdentityProvider newIdp = UserIdentityProvider.builder()
        .userId(userId)
        .provider(provider)
        .providerSubjectId(subjectId)
        .emailAtLogin(email)
        .linkedAt(LocalDateTime.now())
        .lastLoginAt(LocalDateTime.now())
        .build();
    return identityProviderRepository.save(newIdp);
  }
}
