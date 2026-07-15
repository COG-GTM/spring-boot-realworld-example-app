package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void to_string_should_be_millis_of_data() {
    DateTime time = new DateTime(1_600_000_000_000L);
    DateTimeCursor cursor = new DateTimeCursor(time);

    assertThat(cursor.toString()).isEqualTo(String.valueOf(time.getMillis()));
    assertThat(cursor.getData()).isEqualTo(time);
  }

  @Test
  public void parse_should_return_utc_datetime_from_millis_string() {
    long millis = 1_600_000_000_000L;

    DateTime parsed = DateTimeCursor.parse(String.valueOf(millis));

    assertThat(parsed.getMillis()).isEqualTo(millis);
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void parse_should_return_null_for_null_input() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  public void to_string_and_parse_should_round_trip() {
    DateTime time = new DateTime(1_234_567_890L);
    DateTimeCursor cursor = new DateTimeCursor(time);

    DateTime parsed = DateTimeCursor.parse(cursor.toString());

    assertThat(parsed.getMillis()).isEqualTo(time.getMillis());
  }
}
