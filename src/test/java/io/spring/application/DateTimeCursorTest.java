package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_serialize_to_millis() {
    assertEquals("1000", new DateTimeCursor(new DateTime(1000L)).toString());
  }

  @Test
  public void should_parse_millis_into_utc_date_time() {
    DateTime parsed = DateTimeCursor.parse("1000");

    assertEquals(1000L, parsed.getMillis());
    assertEquals(DateTimeZone.UTC, parsed.getZone());
  }

  @Test
  public void should_parse_null_into_null() {
    assertNull(DateTimeCursor.parse(null));
  }
}
