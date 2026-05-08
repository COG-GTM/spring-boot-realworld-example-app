package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_have_default_limit_of_20() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_set_custom_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 50, CursorPager.Direction.NEXT);
    assertEquals(50, param.getLimit());
  }

  @Test
  public void should_cap_limit_at_max_1000() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 2000, CursorPager.Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_not_set_negative_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), -5, CursorPager.Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_not_set_zero_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 0, CursorPager.Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 10, CursorPager.Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  public void should_return_true_for_isNext_when_direction_is_next() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 10, CursorPager.Direction.NEXT);
    assertTrue(param.isNext());
  }

  @Test
  public void should_return_false_for_isNext_when_direction_is_prev() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(new DateTime(), 10, CursorPager.Direction.PREV);
    assertFalse(param.isNext());
  }

  @Test
  public void should_store_cursor_value() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(cursor, 10, CursorPager.Direction.NEXT);
    assertEquals(cursor, param.getCursor());
  }
}
