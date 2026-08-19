package io.spring;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormats {
  public static final DateTimeFormatter UTC_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private DateTimeFormats() {}
}
