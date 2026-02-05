package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_cursor_with_datetime() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData(), is(dateTime));
  }

  @Test
  public void should_convert_to_string_as_millis() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.toString(), is(String.valueOf(dateTime.getMillis())));
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    DateTime result = DateTimeCursor.parse(null);

    assertThat(result, nullValue());
  }

  @Test
  public void should_parse_valid_cursor_string() {
    long millis = 1686825000000L;
    DateTime result = DateTimeCursor.parse(String.valueOf(millis));

    assertThat(result.getMillis(), is(millis));
    assertThat(result.getZone(), is(DateTimeZone.UTC));
  }

  @Test
  public void should_roundtrip_datetime_through_cursor() {
    DateTime original = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    DateTime parsed = DateTimeCursor.parse(cursor.toString());

    assertThat(parsed.getMillis(), is(original.getMillis()));
  }
}
