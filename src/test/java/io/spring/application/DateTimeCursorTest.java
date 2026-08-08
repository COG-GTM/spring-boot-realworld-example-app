package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_render_millis_as_string() {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(1234567L));

    assertThat(cursor.toString()).isEqualTo("1234567");
    assertThat(cursor.getData().getMillis()).isEqualTo(1234567L);
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  public void should_parse_millis_string_into_utc_date_time() {
    DateTime parsed = DateTimeCursor.parse("1000");

    assertThat(parsed).isNotNull();
    assertThat(parsed.getMillis()).isEqualTo(1000L);
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void should_round_trip_through_to_string_and_parse() {
    DateTime original = new DateTime(1600000000000L);

    assertThat(DateTimeCursor.parse(new DateTimeCursor(original).toString()).getMillis())
        .isEqualTo(original.getMillis());
  }
}
