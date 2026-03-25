package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_cursor_with_datetime() {
    DateTime dt = new DateTime(2023, 6, 15, 10, 30);
    DateTimeCursor cursor = new DateTimeCursor(dt);
    assertEquals(dt, cursor.getData());
  }

  @Test
  public void should_convert_to_millis_string() {
    DateTime dt = new DateTime(2023, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dt);
    String str = cursor.toString();
    assertEquals(String.valueOf(dt.getMillis()), str);
  }

  @Test
  public void should_parse_millis_string_to_datetime() {
    DateTime dt = new DateTime(2023, 6, 15, 10, 30, DateTimeZone.UTC);
    String millis = String.valueOf(dt.getMillis());
    DateTime parsed = DateTimeCursor.parse(millis);
    assertNotNull(parsed);
    assertEquals(dt.getMillis(), parsed.getMillis());
  }

  @Test
  public void should_return_null_for_null_cursor() {
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_parse_to_utc_timezone() {
    DateTime dt = new DateTime(2023, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime parsed = DateTimeCursor.parse(String.valueOf(dt.getMillis()));
    assertEquals(DateTimeZone.UTC, parsed.getZone());
  }

  @Test
  public void should_round_trip_cursor() {
    DateTime original = new DateTime(2023, 12, 25, 12, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    DateTime parsed = DateTimeCursor.parse(cursor.toString());
    assertEquals(original.getMillis(), parsed.getMillis());
  }
}
