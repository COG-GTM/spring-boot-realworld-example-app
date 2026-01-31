package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_date_time_cursor() {
    DateTime now = DateTime.now();
    DateTimeCursor cursor = new DateTimeCursor(now);

    assertThat(cursor.getData(), is(now));
  }

  @Test
  public void should_convert_to_string() {
    DateTime now = DateTime.now();
    DateTimeCursor cursor = new DateTimeCursor(now);

    String cursorString = cursor.toString();

    assertThat(cursorString, is(String.valueOf(now.getMillis())));
  }

  @Test
  public void should_parse_cursor_string() {
    DateTime now = DateTime.now().withZone(DateTimeZone.UTC);
    String cursorString = String.valueOf(now.getMillis());

    DateTime parsed = DateTimeCursor.parse(cursorString);

    assertNotNull(parsed);
    assertEquals(now.getMillis(), parsed.getMillis());
  }

  @Test
  public void should_parse_null_cursor() {
    DateTime parsed = DateTimeCursor.parse(null);

    assertNull(parsed);
  }

  @Test
  public void should_round_trip_cursor() {
    DateTime original = DateTime.now();
    DateTimeCursor cursor = new DateTimeCursor(original);

    String cursorString = cursor.toString();
    DateTime parsed = DateTimeCursor.parse(cursorString);

    assertNotNull(parsed);
    assertEquals(original.getMillis(), parsed.getMillis());
  }
}
