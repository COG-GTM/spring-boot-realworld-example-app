package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorCoverageTest {

  @Test
  public void should_create_cursor() {
    DateTime dt = new DateTime(2023, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dt);

    assertNotNull(cursor);
    assertEquals(dt, cursor.getData());
  }

  @Test
  public void should_convert_to_string() {
    DateTime dt = new DateTime(2023, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dt);

    String str = cursor.toString();
    assertNotNull(str);
    assertEquals(String.valueOf(dt.getMillis()), str);
  }

  @Test
  public void should_parse_valid_cursor() {
    DateTime original = new DateTime(2023, 6, 15, 12, 0, DateTimeZone.UTC);
    String cursorStr = String.valueOf(original.getMillis());

    DateTime parsed = DateTimeCursor.parse(cursorStr);

    assertNotNull(parsed);
    assertEquals(original.getMillis(), parsed.getMillis());
  }

  @Test
  public void should_parse_null_returns_null() {
    DateTime parsed = DateTimeCursor.parse(null);
    assertNull(parsed);
  }

  @Test
  public void should_parse_returns_utc() {
    DateTime dt = new DateTime(2023, 1, 1, 0, 0, DateTimeZone.UTC);
    String cursorStr = String.valueOf(dt.getMillis());

    DateTime parsed = DateTimeCursor.parse(cursorStr);

    assertNotNull(parsed);
    assertEquals(DateTimeZone.UTC, parsed.getZone());
  }

  @Test
  public void should_roundtrip() {
    DateTime original = new DateTime(2023, 3, 25, 10, 30, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    String str = cursor.toString();
    DateTime parsed = DateTimeCursor.parse(str);

    assertEquals(original.getMillis(), parsed.getMillis());
  }
}
