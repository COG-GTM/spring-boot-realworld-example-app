package io.spring.application;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_serialize_cursor_as_epoch_millis() {
    DateTime dateTime = new DateTime(1234567890L);

    Assertions.assertEquals("1234567890", new DateTimeCursor(dateTime).toString());
  }

  @Test
  public void should_parse_cursor_to_utc_date_time() {
    DateTime parsed = DateTimeCursor.parse("1234567890");

    Assertions.assertEquals(1234567890L, parsed.getMillis());
    Assertions.assertEquals(DateTimeZone.UTC, parsed.getZone());
  }

  @Test
  public void should_parse_null_cursor_as_null() {
    Assertions.assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_round_trip_cursor() {
    DateTime dateTime = new DateTime();

    Assertions.assertEquals(
        dateTime.getMillis(),
        DateTimeCursor.parse(new DateTimeCursor(dateTime).toString()).getMillis());
  }
}
