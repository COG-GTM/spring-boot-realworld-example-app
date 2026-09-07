package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_render_millis_as_string() {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(12345, DateTimeZone.UTC));

    assertThat(cursor.toString(), is(String.valueOf(12345)));
  }

  @Test
  public void should_parse_null_as_null() {
    assertThat(DateTimeCursor.parse(null), is((DateTime) null));
  }

  @Test
  public void should_parse_millis_in_utc() {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(12345));

    DateTime parsed = DateTimeCursor.parse(cursor.toString());

    assertThat(parsed.getMillis(), is(12345L));
    assertThat(parsed.getZone(), is(DateTimeZone.UTC));
  }

  @Test
  public void should_reject_non_numeric_cursor() {
    org.junit.jupiter.api.Assertions.assertThrows(
        NumberFormatException.class, () -> DateTimeCursor.parse("abc"));
  }
}
