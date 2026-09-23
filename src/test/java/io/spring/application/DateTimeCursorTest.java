package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_use_millis_as_string_representation() {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(123456L));
    assertThat(cursor.toString(), is("123456"));
  }

  @Test
  public void should_parse_millis_into_utc_date_time() {
    DateTime dateTime = DateTimeCursor.parse("123456");

    assertThat(dateTime.getMillis(), is(123456L));
    assertThat(dateTime.getZone(), is(DateTimeZone.UTC));
  }

  @Test
  public void should_parse_null_cursor_into_null() {
    assertThat(DateTimeCursor.parse(null), is(nullValue()));
  }
}
