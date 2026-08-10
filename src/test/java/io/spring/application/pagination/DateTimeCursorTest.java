package io.spring.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

class DateTimeCursorTest {

  @Test
  void should_render_millis_as_string() {
    DateTimeCursor cursor = new DateTimeCursor(new DateTime(123456789L));

    assertThat(cursor.toString()).isEqualTo("123456789");
    assertThat(cursor.getData().getMillis()).isEqualTo(123456789L);
  }

  @Test
  void should_parse_millis_string_into_utc_date_time() {
    DateTime parsed = DateTimeCursor.parse("123456789");

    assertThat(parsed.getMillis()).isEqualTo(123456789L);
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  void should_parse_null_cursor_as_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  void should_round_trip_through_to_string_and_parse() {
    DateTime original = new DateTime(987654321L);

    assertThat(DateTimeCursor.parse(new DateTimeCursor(original).toString()).getMillis())
        .isEqualTo(original.getMillis());
  }
}
