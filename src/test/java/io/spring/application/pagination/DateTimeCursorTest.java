package io.spring.application.pagination;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_cursor_with_datetime() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    Assertions.assertNotNull(cursor);
    Assertions.assertEquals(dateTime, cursor.getData());
  }

  @Test
  public void should_convert_to_string_as_millis() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    String cursorString = cursor.toString();
    Assertions.assertEquals(String.valueOf(dateTime.getMillis()), cursorString);
  }

  @Test
  public void should_parse_cursor_string_to_datetime() {
    DateTime originalDateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    String cursorString = String.valueOf(originalDateTime.getMillis());

    DateTime parsedDateTime = DateTimeCursor.parse(cursorString);

    Assertions.assertNotNull(parsedDateTime);
    Assertions.assertEquals(originalDateTime.getMillis(), parsedDateTime.getMillis());
    Assertions.assertEquals(DateTimeZone.UTC, parsedDateTime.getZone());
  }

  @Test
  public void should_return_null_when_parsing_null_cursor() {
    DateTime parsedDateTime = DateTimeCursor.parse(null);

    Assertions.assertNull(parsedDateTime);
  }

  @Test
  public void should_handle_current_datetime() {
    DateTime now = DateTime.now(DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(now);

    Assertions.assertEquals(now, cursor.getData());
    Assertions.assertEquals(String.valueOf(now.getMillis()), cursor.toString());
  }

  @Test
  public void should_roundtrip_datetime_through_cursor() {
    DateTime originalDateTime = new DateTime(2023, 12, 25, 15, 45, 30, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(originalDateTime);

    String cursorString = cursor.toString();
    DateTime parsedDateTime = DateTimeCursor.parse(cursorString);

    Assertions.assertEquals(originalDateTime.getMillis(), parsedDateTime.getMillis());
  }

  @Test
  public void should_handle_epoch_datetime() {
    DateTime epochDateTime = new DateTime(0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(epochDateTime);

    Assertions.assertEquals("0", cursor.toString());
    DateTime parsed = DateTimeCursor.parse("0");
    Assertions.assertEquals(0, parsed.getMillis());
  }

  @Test
  public void should_handle_far_future_datetime() {
    DateTime futureDateTime = new DateTime(2099, 12, 31, 23, 59, 59, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(futureDateTime);

    String cursorString = cursor.toString();
    DateTime parsedDateTime = DateTimeCursor.parse(cursorString);

    Assertions.assertEquals(futureDateTime.getMillis(), parsedDateTime.getMillis());
  }

  @Test
  public void should_preserve_utc_timezone_on_parse() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.forID("America/New_York"));
    String cursorString = String.valueOf(dateTime.getMillis());

    DateTime parsedDateTime = DateTimeCursor.parse(cursorString);

    Assertions.assertEquals(DateTimeZone.UTC, parsedDateTime.getZone());
    Assertions.assertEquals(dateTime.getMillis(), parsedDateTime.getMillis());
  }
}
