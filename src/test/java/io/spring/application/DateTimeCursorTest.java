package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_encode_cursor_as_epoch_millis() {
    assertEquals(
        "1609502400000", new DateTimeCursor(Instant.parse("2021-01-01T12:00:00Z")).toString());
  }

  @Test
  public void should_round_trip_cursor() {
    Instant instant = Instant.parse("2021-01-01T12:00:00.123Z");
    assertEquals(instant, DateTimeCursor.parse(new DateTimeCursor(instant).toString()));
  }

  @Test
  public void should_parse_null_cursor_to_null() {
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_keep_cursor_ordering_consistent_with_instant_ordering() {
    List<Instant> instants =
        Arrays.asList(
            Instant.parse("2021-01-01T12:00:00.001Z"),
            Instant.parse("2021-01-01T12:00:00.000Z"),
            Instant.parse("2020-06-05T04:03:02.100Z"),
            Instant.parse("2021-01-01T12:00:01.000Z"));

    List<Instant> byInstant =
        instants.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    List<Instant> byCursor =
        instants.stream()
            .sorted(
                Comparator.comparingLong(
                        (Instant i) -> Long.parseLong(new DateTimeCursor(i).toString()))
                    .reversed())
            .collect(Collectors.toList());

    assertEquals(byInstant, byCursor);
    assertTrue(
        Long.parseLong(new DateTimeCursor(instants.get(0)).toString())
            > Long.parseLong(new DateTimeCursor(instants.get(1)).toString()));
  }
}
