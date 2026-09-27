
package org.ptit.meeting.modules.auth.service.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.dto.response.TokenPairDto;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.security.JwtTokenProvider;
import org.ptit.meeting.modules.auth.service.TokenService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

  private static final String REFRESH_TOKEN_PREFIX = "REFRESH_TOKEN:";
  private static final String BLACKLIST_PREFIX = "BLACKLIST_JWT:";
  private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L; // 7 ngày

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public TokenPairDto generateToken(User user, String tokenId) {
    return jwtTokenProvider.generateToken(user, tokenId);
  }

  @Override
  public TokenPairDto createSession(User user, String clientIp, String userAgent) {
    String tokenId = UUID.randomUUID().toString();
    TokenPairDto tokenPair = jwtTokenProvider.generateToken(user, tokenId);

    String key = buildRefreshTokenKey(tokenId);
    log.info("(createSession) Saving refresh token for userId: {}, tokenId: {}", user.getId(), tokenId);
    redisTemplate.opsForValue().set(key, String.valueOf(user.getId()), REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

    return tokenPair;
  }

  @Override
  public boolean isRefreshTokenValid(String refreshToken) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(buildRefreshTokenKey(refreshToken)));
  }

  @Override
  public Long getUserIdByRefreshToken(String refreshToken) {
    Object val = redisTemplate.opsForValue().get(buildRefreshTokenKey(refreshToken));
    if (val != null) {
      try {
        return Long.parseLong(val.toString());
      } catch (NumberFormatException e) {
        log.error("(getUserIdByRefreshToken) Invalid userId stored in Redis: {}", val);
      }
    }
    return null;
  }

  @Override
  public void revokeRefreshToken(String refreshToken) {
    String key = buildRefreshTokenKey(refreshToken);
    log.info("(revokeRefreshToken) Revoking refresh token: {}", refreshToken);
    redisTemplate.delete(key);
  }

  @Override
  public void blacklistToken(String jwtId, long ttlSeconds) {
    String key = buildBlacklistKey(jwtId);
    log.info("(blacklistToken) Blacklisting token: {}, ttl: {}s", jwtId, ttlSeconds);
    redisTemplate.opsForValue().set(key, "REVOKED", ttlSeconds, TimeUnit.SECONDS);
  }

  @Override
  public boolean isTokenBlacklisted(String jwtId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(buildBlacklistKey(jwtId)));
  }

  private String buildRefreshTokenKey(String refreshToken) {
    return REFRESH_TOKEN_PREFIX + refreshToken;
  }

  private String buildBlacklistKey(String jwtId) {
    return BLACKLIST_PREFIX + jwtId;
  }
}
