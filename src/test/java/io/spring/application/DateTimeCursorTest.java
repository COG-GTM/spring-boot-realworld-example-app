package io.spring.application;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_return_epoch_millis_from_toString() {
    Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);
    DateTimeCursor cursor = new DateTimeCursor(instant);
    Assertions.assertEquals("1700000000000", cursor.toString());
  }

  @Test
  public void should_parse_valid_cursor_string() {
    Instant parsed = DateTimeCursor.parse("1700000000000");
    Assertions.assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), parsed);
  }

  @Test
  public void should_return_null_when_parsing_null_cursor() {
    Assertions.assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void should_roundtrip_cursor() {
    Instant original = Instant.ofEpochMilli(1_234_567_890_123L);
    DateTimeCursor cursor = new DateTimeCursor(original);
    Instant parsed = DateTimeCursor.parse(cursor.toString());
    Assertions.assertEquals(original, parsed);
  }
}
