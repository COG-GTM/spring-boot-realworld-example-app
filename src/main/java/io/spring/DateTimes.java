package io.spring;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimes {

  public static final DateTimeFormatter ISO_UTC_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private DateTimes() {}
}
