package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DateTimeFormatterConfigTest {

  @Test
  void formatsZeroMillisWithThreeFractionalDigits() {
    assertEquals(
        "2026-08-27T15:47:01.000Z",
        DateTimeFormatterConfig.UTC_MILLIS_FORMATTER.format(Instant.parse("2026-08-27T15:47:01Z")));
  }
}
