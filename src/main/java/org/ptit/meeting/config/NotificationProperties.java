package org.ptit.meeting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.notification")
public class NotificationProperties {

  private int recipientBatchSize = 500;
  private int maxTargetUsers = 10_000;
  private int defaultPageSize = 20;
  private int maxPageSize = 100;
  private int minReminderMinutes = 30;
  private int maxReminderMinutes = 120;
  private int defaultReminderMinutes = 30;
}
