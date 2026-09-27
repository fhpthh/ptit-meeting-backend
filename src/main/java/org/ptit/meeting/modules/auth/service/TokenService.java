
package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.modules.auth.dto.response.TokenPairDto;
import org.ptit.meeting.modules.auth.entity.User;

public interface TokenService {

  TokenPairDto generateToken(User user, String tokenId);

  TokenPairDto createSession(User user, String clientIp, String userAgent);

  boolean isRefreshTokenValid(String refreshToken);

  Long getUserIdByRefreshToken(String refreshToken);

  void revokeRefreshToken(String refreshToken);

  void blacklistToken(String jwtId, long ttlSeconds);

  boolean isTokenBlacklisted(String jwtId);
}
