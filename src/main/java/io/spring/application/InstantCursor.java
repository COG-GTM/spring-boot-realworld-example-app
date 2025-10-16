package io.spring.application;

import java.time.Instant;

public class InstantCursor extends PageCursor<Instant> {

  public InstantCursor(Instant data) {
    super(data);
  }

  @Override
  public String toString() {
    return String.valueOf(getData().toEpochMilli());
  }

  public static Instant parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    return Instant.ofEpochMilli(Long.parseLong(cursor));
  }
}
