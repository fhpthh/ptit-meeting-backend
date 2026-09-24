package org.ptit.meeting.layer.application.port.outbound;

import org.ptit.meeting.layer.application.dto.TokenPairDto;
import org.ptit.meeting.layer.domain.model.User;

public interface TokenProviderPort {

  TokenPairDto generateToken(User user, String tokenId);

  String generateFirstLoginToken(User user);

  boolean validateToken(String token);

  Long extractUserId(String token);

  String extractJwtId(String token);

  long getRemainingExpirationMs(String token);
}
