package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_keep_valid_limit_and_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    assertEquals(10, parameter.getLimit());
    assertEquals(11, parameter.getQueryLimit());
    assertEquals(cursor, parameter.getCursor());
    assertTrue(parameter.isNext());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> parameter =
        new CursorPageParameter<>(null, 10000, Direction.PREV);

    assertEquals(1000, parameter.getLimit());
    assertNull(parameter.getCursor());
    assertFalse(parameter.isNext());
  }

  @Test
  public void should_fall_back_to_default_limit_for_non_positive_value() {
    assertEquals(20, new CursorPageParameter<DateTime>(null, 0, Direction.NEXT).getLimit());
  }
}
