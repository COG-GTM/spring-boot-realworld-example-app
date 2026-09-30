package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void parses_null_and_numeric_cursors() {
    assertNull(DateTimeCursor.parse(null));
    DateTime parsed = DateTimeCursor.parse("1600000000000");
    assertEquals(1600000000000L, parsed.getMillis());
    assertEquals("1600000000000", new DateTimeCursor(parsed).toString());
  }

  @Test
  public void rejects_malformed_cursors() {
    for (String cursor : new String[] {"", "x", "1.5", "99999999999999999999"}) {
      assertThrows(InvalidCursorException.class, () -> DateTimeCursor.parse(cursor));
    }
  }
}
