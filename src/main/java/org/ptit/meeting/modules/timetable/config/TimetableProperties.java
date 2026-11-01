package org.ptit.meeting.modules.timetable.config;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.timetable")
public class TimetableProperties {

  private Map<Integer, PeriodConfig> periods = new HashMap<>();

  @Getter
  @Setter
  public static class PeriodConfig {

    private LocalTime start;
    private LocalTime end;
  }
}
