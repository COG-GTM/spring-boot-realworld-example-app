package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DateTimesTest {

  @Test
  void formatsZeroMillisWithThreeFractionalDigits() {
    assertEquals(
        "2026-08-27T15:47:01.000Z",
        DateTimes.ISO_UTC_MILLIS.format(Instant.parse("2026-08-27T15:47:01Z")));
  }
}
