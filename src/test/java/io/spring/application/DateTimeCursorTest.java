package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_serialize_to_millis() {
    assertThat(new DateTimeCursor(new DateTime(1234567L)).toString(), is("1234567"));
  }

  @Test
  public void should_parse_millis_back_to_date_time() {
    assertThat(DateTimeCursor.parse("1234567").getMillis(), is(1234567L));
  }

  @Test
  public void should_parse_null_cursor_as_null() {
    assertThat(DateTimeCursor.parse(null), nullValue());
  }
}
