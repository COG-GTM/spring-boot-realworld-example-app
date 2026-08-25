package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_render_millis_as_string() {
    DateTime dateTime = new DateTime(2020, 1, 2, 3, 4, 5, 0, DateTimeZone.UTC);

    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData()).isEqualTo(dateTime);
    assertThat(cursor.toString()).isEqualTo(String.valueOf(dateTime.getMillis()));
  }

  @Test
  public void should_parse_millis_string_into_utc_datetime() {
    DateTime dateTime = new DateTime(2020, 1, 2, 3, 4, 5, 0, DateTimeZone.UTC);

    DateTime parsed = DateTimeCursor.parse(String.valueOf(dateTime.getMillis()));

    assertThat(parsed.getMillis()).isEqualTo(dateTime.getMillis());
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void should_parse_null_cursor_into_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }
}
