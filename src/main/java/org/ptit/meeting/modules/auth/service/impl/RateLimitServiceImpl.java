
package org.ptit.meeting.modules.auth.service.impl;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.service.RateLimitService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

  private static final String LOGIN_FAIL_PREFIX = "LOGIN_FAIL:";
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final long LOCK_WINDOW_SECONDS = 15 * 60L; // 15 phút

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void checkRateLimit(String identifier) {
    Object val = redisTemplate.opsForValue().get(buildKey(identifier));
    if (val instanceof Number number && number.intValue() >= MAX_FAILED_ATTEMPTS) {
      log.warn("(checkRateLimit) Rate limit exceeded for identifier: {}", identifier);
      throw new BusinessException(
          StatusCode.RATE_LIMIT_EXCEEDED,
          AuthErrorConstants.RATE_LIMIT_EXCEEDED
      );
    }
  }

  @Override
  public void recordLoginFailure(String identifier) {
    String key = buildKey(identifier);
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
      redisTemplate.expire(key, LOCK_WINDOW_SECONDS, TimeUnit.SECONDS);
    }
    log.warn("(recordLoginFailure) Recorded login failure for identifier: {}, current count: {}", identifier, count);
  }

  @Override
  public void resetLoginFailures(String identifier) {
    log.info("(resetLoginFailures) Resetting login failures for identifier: {}", identifier);
    redisTemplate.delete(buildKey(identifier));
  }

  private String buildKey(String identifier) {
    return LOGIN_FAIL_PREFIX + identifier;
  }
}
