package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_render_millis_as_string() {
    DateTime time = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(time);
    assertThat(cursor.toString(), is(String.valueOf(time.getMillis())));
    assertThat(cursor.getData(), is(time));
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertThat(DateTimeCursor.parse(null), is(nullValue()));
  }

  @Test
  public void should_round_trip_millis_via_parse() {
    DateTime time = new DateTime(2020, 6, 15, 12, 30, DateTimeZone.UTC);
    String rendered = new DateTimeCursor(time).toString();
    DateTime parsed = DateTimeCursor.parse(rendered);
    assertThat(parsed.getMillis(), is(time.getMillis()));
    assertThat(parsed.getZone(), is(DateTimeZone.UTC));
  }
}
