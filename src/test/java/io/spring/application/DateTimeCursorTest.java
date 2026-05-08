package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_convert_to_string_as_millis() {
    DateTime dt = new DateTime(1609459200000L, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dt);
    assertEquals("1609459200000", cursor.toString());
  }

  @Test
  public void should_return_data() {
    DateTime dt = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(dt);
    assertEquals(dt, cursor.getData());
  }

  @Test
  public void should_parse_valid_cursor_string() {
    DateTime result = DateTimeCursor.parse("1609459200000");
    assertNotNull(result);
    assertEquals(1609459200000L, result.getMillis());
    assertEquals(DateTimeZone.UTC, result.getZone());
  }

  @Test
  public void should_return_null_for_null_cursor() {
    DateTime result = DateTimeCursor.parse(null);
    assertNull(result);
  }

  @Test
  public void should_roundtrip_through_toString_and_parse() {
    DateTime original = new DateTime(2021, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    DateTime parsed = DateTimeCursor.parse(cursor.toString());
    assertEquals(original.getMillis(), parsed.getMillis());
  }
}
