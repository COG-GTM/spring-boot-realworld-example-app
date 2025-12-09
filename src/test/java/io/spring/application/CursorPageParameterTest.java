package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_default_values() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();

    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
    assertNull(param.getDirection());
  }

  @Test
  public void should_create_with_custom_values() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 50, Direction.NEXT);

    assertEquals(50, param.getLimit());
    assertEquals(cursor, param.getCursor());
    assertEquals(Direction.NEXT, param.getDirection());
  }

  @Test
  public void should_return_true_for_next_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.NEXT);

    assertTrue(param.isNext());
  }

  @Test
  public void should_return_false_for_prev_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.PREV);

    assertFalse(param.isNext());
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.NEXT);

    assertEquals(21, param.getQueryLimit());
  }

  @Test
  public void should_cap_limit_at_max_1000() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 1500, Direction.NEXT);

    assertEquals(1000, param.getLimit());
    assertEquals(1001, param.getQueryLimit());
  }

  @Test
  public void should_not_set_negative_limit() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, -10, Direction.NEXT);

    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_not_set_zero_limit() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 0, Direction.NEXT);

    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_accept_limit_at_max_boundary() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 1000, Direction.NEXT);

    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_return_false_for_null_direction() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, null);

    assertFalse(param.isNext());
  }
}
