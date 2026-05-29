package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class PageCursorTest {

  @Test
  void should_get_data() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);
    assertEquals(now, cursor.getData());
  }

  @Test
  void should_return_string_representation() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);
    assertNotNull(cursor.toString());
  }
}
