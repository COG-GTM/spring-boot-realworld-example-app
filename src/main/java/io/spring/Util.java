package io.spring;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Util {
  public static final DateTimeFormatter ISO_UTC_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
