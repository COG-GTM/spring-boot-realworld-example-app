package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_cursor_with_datetime() {
    DateTime time = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(time);

    assertEquals(time, cursor.getData());
  }

  @Test
  public void should_convert_to_string_as_millis() {
    DateTime time = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(time);

    assertEquals(String.valueOf(time.getMillis()), cursor.toString());
  }

  @Test
  public void should_parse_string_to_datetime() {
    DateTime originalTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    String cursorString = String.valueOf(originalTime.getMillis());

    DateTime parsedTime = DateTimeCursor.parse(cursorString);

    assertEquals(originalTime.getMillis(), parsedTime.getMillis());
    assertEquals(DateTimeZone.UTC, parsedTime.getZone());
  }

  @Test
  public void should_return_null_when_parsing_null_string() {
    DateTime result = DateTimeCursor.parse(null);

    assertNull(result);
  }

  @Test
  public void should_roundtrip_through_string_and_parse() {
    DateTime originalTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(originalTime);

    String stringValue = cursor.toString();
    DateTime parsedTime = DateTimeCursor.parse(stringValue);

    assertEquals(originalTime.getMillis(), parsedTime.getMillis());
  }

  @Test
  public void should_handle_epoch_time() {
    DateTime epochTime = new DateTime(0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(epochTime);

    assertEquals("0", cursor.toString());
    assertEquals(0, DateTimeCursor.parse("0").getMillis());
  }

  @Test
  public void should_handle_current_time() {
    DateTime now = DateTime.now(DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(now);

    DateTime parsed = DateTimeCursor.parse(cursor.toString());
    assertEquals(now.getMillis(), parsed.getMillis());
  }
}
