package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_serialize_to_epoch_millis() {
    DateTime dateTime = new DateTime(2020, 1, 2, 3, 4, 5, DateTimeZone.UTC);

    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData(), is(dateTime));
    assertThat(cursor.toString(), is(String.valueOf(dateTime.getMillis())));
  }

  @Test
  public void should_round_trip_cursor_value() {
    DateTime dateTime = new DateTime(2021, 6, 7, 8, 9, 10, DateTimeZone.UTC);

    DateTime parsed = DateTimeCursor.parse(new DateTimeCursor(dateTime).toString());

    assertThat(parsed.getMillis(), is(dateTime.getMillis()));
    assertThat(parsed.getZone(), is(DateTimeZone.UTC));
    assertThat(parsed, is(dateTime));
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertThat(DateTimeCursor.parse(null), nullValue());
  }

  @Test
  public void should_throw_on_invalid_cursor() {
    assertThrows(NumberFormatException.class, () -> DateTimeCursor.parse("not-a-timestamp"));
  }
}
