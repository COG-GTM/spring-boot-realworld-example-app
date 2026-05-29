package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  void should_create_with_defaults() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
  }

  @Test
  void should_create_with_params() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(cursor, 10, CursorPager.Direction.NEXT);
    assertEquals(10, param.getLimit());
    assertEquals(cursor, param.getCursor());
    assertTrue(param.isNext());
  }

  @Test
  void should_return_query_limit_plus_one() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 2000, CursorPager.Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  void should_not_allow_negative_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, -1, CursorPager.Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  void should_identify_prev_direction() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 10, CursorPager.Direction.PREV);
    assertFalse(param.isNext());
  }
}
