package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_expose_the_wrapped_date_time() {
    DateTime now = new DateTime();

    assertThat(new DateTimeCursor(now).getData()).isEqualTo(now);
  }

  @Test
  public void should_render_epoch_millis_as_string() {
    DateTime now = new DateTime();

    assertThat(new DateTimeCursor(now).toString()).isEqualTo(String.valueOf(now.getMillis()));
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  public void should_round_trip_a_cursor_string() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);

    DateTime parsed = DateTimeCursor.parse(cursor.toString());

    assertThat(parsed.getMillis()).isEqualTo(now.getMillis());
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void should_parse_epoch_zero() {
    DateTime parsed = DateTimeCursor.parse("0");

    assertThat(parsed.getMillis()).isZero();
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void should_fail_to_parse_non_numeric_cursor() {
    assertThatThrownBy(() -> DateTimeCursor.parse("not-a-number"))
        .isInstanceOf(NumberFormatException.class);
  }
}
