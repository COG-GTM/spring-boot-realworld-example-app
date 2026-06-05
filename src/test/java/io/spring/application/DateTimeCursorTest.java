package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_store_datetime_data() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);
    assertThat(cursor.getData(), is(now));
  }

  @Test
  public void should_convert_to_string_as_millis() {
    DateTime dt = new DateTime(1609459200000L);
    DateTimeCursor cursor = new DateTimeCursor(dt);
    assertThat(cursor.toString(), is("1609459200000"));
  }

  @Test
  public void should_parse_cursor_string_to_datetime() {
    DateTime parsed = DateTimeCursor.parse("1609459200000");
    assertThat(parsed, notNullValue());
    assertThat(parsed.getMillis(), is(1609459200000L));
    assertThat(parsed.getZone(), is(DateTimeZone.UTC));
  }

  @Test
  public void should_return_null_when_parsing_null() {
    DateTime parsed = DateTimeCursor.parse(null);
    assertThat(parsed, nullValue());
  }

  @Test
  public void should_roundtrip_cursor_to_string_and_back() {
    DateTime original = new DateTime(2025, 1, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    String cursorString = cursor.toString();
    DateTime parsed = DateTimeCursor.parse(cursorString);
    assertThat(parsed.getMillis(), is(original.getMillis()));
  }
}
