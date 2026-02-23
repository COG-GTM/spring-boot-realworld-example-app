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
  public void should_return_millis_as_string() {
    DateTime dateTime = new DateTime(1000000L, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.toString(), is("1000000"));
  }

  @Test
  public void should_return_data() {
    DateTime dateTime = new DateTime(1000000L, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData(), is(dateTime));
  }

  @Test
  public void should_parse_valid_cursor_string() {
    DateTime result = DateTimeCursor.parse("1000000");

    assertThat(result, notNullValue());
    assertThat(result.getMillis(), is(1000000L));
    assertThat(result.getZone(), is(DateTimeZone.UTC));
  }

  @Test
  public void should_return_null_for_null_cursor() {
    DateTime result = DateTimeCursor.parse(null);

    assertThat(result, nullValue());
  }

  @Test
  public void should_roundtrip_through_toString_and_parse() {
    DateTime original = new DateTime(DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(original);
    String serialized = cursor.toString();
    DateTime parsed = DateTimeCursor.parse(serialized);

    assertThat(parsed.getMillis(), is(original.getMillis()));
  }
}
