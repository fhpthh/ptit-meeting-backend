package org.ptit.meeting.integration.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.ptit.meeting.config.AiRecordingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "application.ai-recording.dispatch",
    name = "enabled",
    havingValue = "true")
public class RedisRecordingDispatchPublisher implements RecordingDispatchPublisher {
  private static final DefaultRedisScript<String> APPEND =
      new DefaultRedisScript<>(
          "return redis.call('XADD',KEYS[1],'MAXLEN','=',ARGV[1],'*',unpack(ARGV,2))",
          String.class);
  private final StringRedisTemplate redis;
  private final String stream;
  private final int maxLength;

  public RedisRecordingDispatchPublisher(
      StringRedisTemplate redis, AiRecordingProperties properties) {
    var config = properties.getDispatch();
    if (config.getStream() == null
        || !config.getStream().matches("[a-zA-Z0-9:_-]{1,128}")
        || config.getMaxLength() < 1
        || config.getMaxLength() > 1000000)
      throw new IllegalStateException("Invalid recording dispatch stream configuration");
    this.redis = redis;
    stream = config.getStream();
    maxLength = config.getMaxLength();
  }

  @Override
  public String publish(Map<String, String> fields) {
    if (fields.isEmpty() || fields.size() > 16)
      throw new IllegalArgumentException("Invalid dispatch envelope");
    var args = new ArrayList<String>();
    args.add(Integer.toString(maxLength));
    new TreeMap<>(fields)
        .forEach(
            (key, value) -> {
              if (key == null || value == null || key.length() > 64 || value.length() > 128)
                throw new IllegalArgumentException("Invalid dispatch field");
              args.add(key);
              args.add(value);
            });
    String id = redis.execute(APPEND, List.of(stream), args.toArray());
    if (id == null || !id.matches("[0-9]+-[0-9]+"))
      throw new IllegalStateException("Redis dispatch ACK missing");
    return id;
  }
}
