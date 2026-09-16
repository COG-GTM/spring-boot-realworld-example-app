package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_serialize_to_millis() {
    DateTime time = new DateTime(1500000000000L);
    assertEquals("1500000000000", new DateTimeCursor(time).toString());
    assertEquals(time, new DateTimeCursor(time).getData());
  }

  @Test
  public void should_parse_millis_to_utc_datetime() {
    DateTime parsed = DateTimeCursor.parse("1500000000000");
    assertEquals(1500000000000L, parsed.getMillis());
    assertEquals(DateTimeZone.UTC, parsed.getZone());
  }

  @Test
  public void should_round_trip() {
    DateTime time = new DateTime();
    DateTime parsed = DateTimeCursor.parse(new DateTimeCursor(time).toString());
    assertEquals(time.getMillis(), parsed.getMillis());
  }

  @Test
  public void should_return_null_for_null_cursor() {
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_throw_for_non_numeric_cursor() {
    assertThrows(NumberFormatException.class, () -> DateTimeCursor.parse("not-a-number"));
  }
}
