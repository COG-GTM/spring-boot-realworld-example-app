package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  void should_create_and_get_data() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);
    assertEquals(now, cursor.getData());
  }

  @Test
  void should_to_string_as_millis() {
    DateTime dateTime = new DateTime(2022, 1, 15, 10, 0, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);
    assertEquals(String.valueOf(dateTime.getMillis()), cursor.toString());
  }

  @Test
  void should_parse_cursor_string() {
    DateTime dateTime = new DateTime(2022, 1, 15, 10, 0, 0, DateTimeZone.UTC);
    String cursorStr = String.valueOf(dateTime.getMillis());
    DateTime parsed = DateTimeCursor.parse(cursorStr);
    assertNotNull(parsed);
    assertEquals(dateTime.getMillis(), parsed.getMillis());
  }

  @Test
  void should_return_null_for_null_cursor() {
    assertNull(DateTimeCursor.parse(null));
  }
}
