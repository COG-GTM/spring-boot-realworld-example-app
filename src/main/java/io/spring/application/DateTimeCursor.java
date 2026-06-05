package io.spring.application;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DateTimeCursor extends PageCursor<OffsetDateTime> {

  public DateTimeCursor(OffsetDateTime data) {
    super(data);
  }

  @Override
  public String toString() {
    return String.valueOf(getData().toInstant().toEpochMilli());
  }

  public static OffsetDateTime parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(cursor)), ZoneOffset.UTC);
  }
}
