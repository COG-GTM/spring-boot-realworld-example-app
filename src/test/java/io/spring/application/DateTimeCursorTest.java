package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DateTimeCursorTest {

  @Test
  public void should_encode_the_date_time_as_epoch_millis() {
    DateTime dateTime = new DateTime(1_600_000_000_123L, DateTimeZone.UTC);

    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData()).isEqualTo(dateTime);
    assertThat(cursor.toString()).isEqualTo("1600000000123");
  }

  @Test
  public void should_encode_the_same_instant_regardless_of_time_zone() {
    DateTime utc = new DateTime(1_600_000_000_000L, DateTimeZone.UTC);
    DateTime shifted = utc.withZone(DateTimeZone.forOffsetHours(8));

    assertThat(new DateTimeCursor(shifted).toString())
        .isEqualTo(new DateTimeCursor(utc).toString());
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, 1L, 1_600_000_000_000L, 4_102_444_800_000L})
  public void should_round_trip_through_parse(long millis) {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(millis, DateTimeZone.UTC));

    DateTime parsed = DateTimeCursor.parse(cursor.toString());

    assertThat(parsed.getMillis()).isEqualTo(millis);
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  public void should_reject_a_non_numeric_cursor() {
    assertThatThrownBy(() -> DateTimeCursor.parse("not-a-number"))
        .isInstanceOf(NumberFormatException.class);
  }

  @Test
  public void should_fail_to_render_a_cursor_without_data() {
    DateTimeCursor cursor = new DateTimeCursor(null);

    assertThatThrownBy(cursor::toString).isInstanceOf(NullPointerException.class);
  }
}
