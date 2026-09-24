package org.ptit.meeting.layer.application.port.outbound;

public interface TokenCachePort {

  void saveRefreshToken(Long userId, String tokenId, String clientIp, String userAgent,
      long ttlSeconds);

  boolean hasRefreshToken(Long userId, String tokenId);

  void deleteRefreshToken(Long userId, String tokenId);

  void revokeAllUserSessions(Long userId);

  void blacklistToken(String jwtId, long remainingMs);

  boolean isTokenBlacklisted(String jwtId);

  void recordLoginFailure(String identifier);

  int getLoginFailures(String identifier);

  void resetLoginFailures(String identifier);
}
