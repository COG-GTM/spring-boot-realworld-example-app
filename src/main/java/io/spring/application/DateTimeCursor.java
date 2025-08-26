package io.spring.application;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateTimeCursor extends PageCursor<ZonedDateTime> {

  public DateTimeCursor(ZonedDateTime data) {
    super(data);
  }

  @Override
  public String toString() {
    return String.valueOf(getData().toInstant().toEpochMilli());
  }

  public static ZonedDateTime parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(cursor)), ZoneOffset.UTC);
  }
}
