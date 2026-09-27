package org.ptit.meeting.service;

import org.ptit.meeting.dto.response.TokenPairDto;
import org.ptit.meeting.entity.User;

public interface TokenService {

  TokenPairDto generateToken(User user, String tokenId);

  TokenPairDto createSession(User user, String clientIp, String userAgent);

  boolean isRefreshTokenValid(String refreshToken);

  Long getUserIdByRefreshToken(String refreshToken);

  void revokeRefreshToken(String refreshToken);

  void blacklistToken(String jwtId, long ttlSeconds);

  boolean isTokenBlacklisted(String jwtId);
}
