package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_return_millis_as_string() {
    DateTime dateTime = new DateTime(1234567890L, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);
    assertEquals("1234567890", cursor.toString());
  }

  @Test
  public void should_parse_cursor_string() {
    DateTime result = DateTimeCursor.parse("1234567890");
    assertNotNull(result);
    assertEquals(1234567890L, result.getMillis());
  }

  @Test
  public void should_return_null_when_parsing_null() {
    DateTime result = DateTimeCursor.parse(null);
    assertNull(result);
  }

  @Test
  public void should_get_data() {
    DateTime dateTime = new DateTime(DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);
    assertEquals(dateTime, cursor.getData());
  }
}
