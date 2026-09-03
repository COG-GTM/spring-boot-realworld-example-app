package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_render_millis_as_string() {
    DateTime dateTime = new DateTime(12345L);

    assertEquals(String.valueOf(dateTime.getMillis()), new DateTimeCursor(dateTime).toString());
  }

  @Test
  public void should_parse_null_as_null() {
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_parse_millis_in_utc() {
    DateTime dateTime = new DateTime(12345L, DateTimeZone.forOffsetHours(2));

    DateTime parsed = DateTimeCursor.parse(new DateTimeCursor(dateTime).toString());

    assertEquals(dateTime.getMillis(), parsed.getMillis());
    assertEquals(DateTimeZone.UTC, parsed.getZone());
  }
}
