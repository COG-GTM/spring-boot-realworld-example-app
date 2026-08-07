package io.spring;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimes {

  /** ISO-8601 in UTC with exactly three fractional-second digits, e.g. 2021-01-01T12:00:00.000Z. */
  public static final DateTimeFormatter ISO_UTC =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneOffset.UTC);

  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }

  public static String format(Instant instant) {
    return ISO_UTC.format(instant);
  }
}
