package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_create_cursor_from_instant() {
    Instant instant = Instant.ofEpochMilli(1734629525403L);
    DateTimeCursor cursor = new DateTimeCursor(instant);
    assertEquals(instant, cursor.getData());
  }

  @Test
  public void should_convert_cursor_to_string_as_epoch_millis() {
    Instant instant = Instant.ofEpochMilli(1734629525403L);
    DateTimeCursor cursor = new DateTimeCursor(instant);
    assertEquals("1734629525403", cursor.toString());
  }

  @Test
  public void should_parse_cursor_string_to_instant() {
    Instant result = DateTimeCursor.parse("1734629525403");
    assertEquals(Instant.ofEpochMilli(1734629525403L), result);
  }

  @Test
  public void should_return_null_when_parsing_null_cursor() {
    Instant result = DateTimeCursor.parse(null);
    assertNull(result);
  }

  @Test
  public void should_roundtrip_instant_through_cursor() {
    Instant original = Instant.now();
    DateTimeCursor cursor = new DateTimeCursor(original);
    String cursorString = cursor.toString();
    Instant parsed = DateTimeCursor.parse(cursorString);
    assertEquals(original.toEpochMilli(), parsed.toEpochMilli());
  }

  @Test
  public void should_preserve_epoch_millis_precision() {
    long epochMillis = 1734629525403L;
    Instant original = Instant.ofEpochMilli(epochMillis);
    DateTimeCursor cursor = new DateTimeCursor(original);
    Instant parsed = DateTimeCursor.parse(cursor.toString());
    assertEquals(epochMillis, parsed.toEpochMilli());
  }

  @Test
  public void should_throw_exception_for_invalid_cursor_string() {
    assertThrows(NumberFormatException.class, () -> {
      DateTimeCursor.parse("invalid-cursor");
    });
  }

  @Test
  public void should_throw_exception_for_empty_cursor_string() {
    assertThrows(NumberFormatException.class, () -> {
      DateTimeCursor.parse("");
    });
  }

  @Test
  public void should_handle_zero_epoch_millis() {
    Instant epoch = Instant.EPOCH;
    DateTimeCursor cursor = new DateTimeCursor(epoch);
    assertEquals("0", cursor.toString());
    assertEquals(epoch, DateTimeCursor.parse("0"));
  }

  @Test
  public void should_handle_negative_epoch_millis() {
    Instant beforeEpoch = Instant.ofEpochMilli(-1000L);
    DateTimeCursor cursor = new DateTimeCursor(beforeEpoch);
    assertEquals("-1000", cursor.toString());
    assertEquals(beforeEpoch, DateTimeCursor.parse("-1000"));
  }
}
