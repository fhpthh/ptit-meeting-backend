package org.ptit.meeting.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DataUtils {
  public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

  private DataUtils() {
  }

  public static Instant now() {
    return Instant.now();
  }

  /**
   * Chuỗi ISO-8601 (UTC) của thời điểm hiện tại.
   */
  public static String nowIso() {
    return ISO.format(Instant.now());
  }

  public static String format(Instant instant) {
    return instant == null ? null : ISO.format(instant);
  }

  /**
   * Đổi {@link Instant} (UTC) sang {@link LocalDateTime} theo zone mặc định để hiển thị.
   */
  public static LocalDateTime toLocal(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, DEFAULT_ZONE);
  }

  public static Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
  }
}
