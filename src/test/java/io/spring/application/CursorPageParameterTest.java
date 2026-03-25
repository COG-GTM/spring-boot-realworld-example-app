package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_defaults() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
    assertNull(param.getDirection());
  }

  @Test
  public void should_create_with_all_params() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 10, CursorPager.Direction.NEXT);
    assertEquals(10, param.getLimit());
    assertEquals(cursor, param.getCursor());
    assertEquals(CursorPager.Direction.NEXT, param.getDirection());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 2000, CursorPager.Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_keep_default_for_zero_limit() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 0, CursorPager.Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_keep_default_for_negative_limit() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, -5, CursorPager.Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  public void should_return_is_next_true_for_next_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT);
    assertTrue(param.isNext());
  }

  @Test
  public void should_return_is_next_false_for_prev_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, CursorPager.Direction.PREV);
    assertFalse(param.isNext());
  }

  @Test
  public void should_set_valid_limit() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 50, CursorPager.Direction.NEXT);
    assertEquals(50, param.getLimit());
  }
}
