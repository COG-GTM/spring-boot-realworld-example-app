package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_defaults() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
  }

  @Test
  public void should_create_with_parameters() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    assertEquals(10, param.getLimit());
    assertEquals(cursor, param.getCursor());
    assertEquals(Direction.NEXT, param.getDirection());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 2000, Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_keep_default_limit_when_negative() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, -1, Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_keep_default_limit_when_zero() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 0, Direction.NEXT);
    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  public void should_return_true_for_is_next_when_direction_is_next() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertTrue(param.isNext());
  }

  @Test
  public void should_return_false_for_is_next_when_direction_is_prev() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.PREV);
    assertFalse(param.isNext());
  }
}
