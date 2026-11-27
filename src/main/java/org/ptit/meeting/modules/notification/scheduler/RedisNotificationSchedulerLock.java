package org.ptit.meeting.modules.notification.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSchedulerLock implements NotificationSchedulerLock {

  private static final String FALLBACK_LOCAL_TOKEN = "LOCAL_FALLBACK";
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
      Long.class
  );

  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public String tryAcquire(String lockKey, Duration ttl) {
    String token = UUID.randomUUID().toString();
    try {
      Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl);
      return Boolean.TRUE.equals(acquired) ? token : null;
    } catch (DataAccessException ex) {
      log.warn("Redis unavailable while acquiring scheduler lock '{}'; proceeding in local mode: {}",
          lockKey, ex.getMessage());
      return FALLBACK_LOCAL_TOKEN;
    }
  }

  @Override
  public void release(String lockKey, String token) {
    if (token == null || FALLBACK_LOCAL_TOKEN.equals(token)) {
      return;
    }
    try {
      stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(lockKey), token);
    } catch (DataAccessException ex) {
      log.warn("Redis unavailable while releasing scheduler lock '{}': {}",
          lockKey, ex.getMessage());
    }
  }
}
