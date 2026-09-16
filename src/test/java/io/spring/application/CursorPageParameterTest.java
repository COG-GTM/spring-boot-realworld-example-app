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
  public void should_use_defaults_with_no_args_constructor() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
    assertNull(param.getDirection());
    assertFalse(param.isNext());
  }

  @Test
  public void should_set_cursor_limit_and_direction() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    assertEquals(cursor, param.getCursor());
    assertEquals(10, param.getLimit());
    assertEquals(Direction.NEXT, param.getDirection());
    assertTrue(param.isNext());
  }

  @Test
  public void should_not_be_next_for_prev_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.PREV);
    assertFalse(param.isNext());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 5000, Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_keep_default_limit_for_non_positive_limit() {
    assertEquals(20, new CursorPageParameter<DateTime>(null, 0, Direction.NEXT).getLimit());
    assertEquals(20, new CursorPageParameter<DateTime>(null, -3, Direction.NEXT).getLimit());
  }

  @Test
  public void should_query_one_more_than_limit_to_detect_extra_page() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }
}
