package org.ptit.meeting.modules.meeting.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.meeting.service.MeetingHostPresenceService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingHostPresenceServiceImpl implements MeetingHostPresenceService {

  private static final String KEY_PREFIX = "mtg:ha:";
  private static final Duration RECONNECT_GRACE = Duration.ofMinutes(2);
  private static final Duration STATE_TTL = Duration.ofDays(1);

  // Compare before writing: a host admission invalidates snapshots captured before it.
  private static final DefaultRedisScript<Long> OBSERVE_ABSENCE_SCRIPT =
      new DefaultRedisScript<>("""
          local state = redis.call('GET', KEYS[1]) or ''
          if state ~= ARGV[1] then
            return nil
          end
          local since = tonumber(state)
          if not since then
            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
            return tonumber(ARGV[2])
          end
          return since
          """, Long.class);

  private final StringRedisTemplate redisTemplate;

  @Override
  public String getState(String runtimeSessionId) {
    return Objects.requireNonNullElse(redisTemplate.opsForValue().get(key(runtimeSessionId)), "");
  }

  @Override
  public boolean hasReconnectGracePassed(String runtimeSessionId, String observedState, Instant now) {
    Long absentSince = redisTemplate.execute(
        OBSERVE_ABSENCE_SCRIPT,
        List.of(key(runtimeSessionId)),
        observedState,
        Long.toString(now.toEpochMilli()),
        Long.toString(STATE_TTL.toSeconds())
    );
    return absentSince != null
        && !now.isBefore(Instant.ofEpochMilli(absentSince).plus(RECONNECT_GRACE));
  }

  @Override
  public void markPresent(String runtimeSessionId) {
    // Keep a fresh marker rather than deleting: old absence snapshots must stay invalid.
    redisTemplate.opsForValue().set(key(runtimeSessionId), "p:" + UUID.randomUUID(), STATE_TTL);
  }

  @Override
  public void clear(String runtimeSessionId) {
    redisTemplate.delete(key(runtimeSessionId));
  }

  private String key(String runtimeSessionId) {
    return KEY_PREFIX + runtimeSessionId;
  }
}
