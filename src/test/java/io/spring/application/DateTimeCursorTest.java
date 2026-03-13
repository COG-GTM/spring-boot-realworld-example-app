package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_return_millis_as_string() {
    DateTime dateTime = new DateTime(1234567890L, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);
    assertThat(cursor.toString(), is("1234567890"));
  }

  @Test
  public void should_return_null_when_parsing_null() {
    assertThat(DateTimeCursor.parse(null), is(nullValue()));
  }

  @Test
  public void should_parse_valid_millis_string() {
    DateTime result = DateTimeCursor.parse("1234567890");
    assertThat(result.getMillis(), is(1234567890L));
    assertThat(result.getZone(), is(DateTimeZone.UTC));
  }
}
