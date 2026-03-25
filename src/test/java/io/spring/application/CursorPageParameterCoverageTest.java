package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterCoverageTest {

  @Test
  public void should_create_with_defaults() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
    assertNull(param.getCursor());
    assertNull(param.getDirection());
  }

  @Test
  public void should_create_with_constructor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(cursor, 10, CursorPager.Direction.NEXT);

    assertEquals(10, param.getLimit());
    assertEquals(cursor, param.getCursor());
    assertEquals(CursorPager.Direction.NEXT, param.getDirection());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 2000, CursorPager.Direction.NEXT);

    assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_not_accept_negative_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, -1, CursorPager.Direction.NEXT);

    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_not_accept_zero_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 0, CursorPager.Direction.NEXT);

    assertEquals(20, param.getLimit());
  }

  @Test
  public void should_accept_positive_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 50, CursorPager.Direction.NEXT);

    assertEquals(50, param.getLimit());
  }

  @Test
  public void should_return_query_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);

    assertEquals(21, param.getQueryLimit());
  }

  @Test
  public void should_check_is_next() {
    CursorPageParameter<DateTime> paramNext =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    CursorPageParameter<DateTime> paramPrev =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.PREV);

    assertTrue(paramNext.isNext());
    assertFalse(paramPrev.isNext());
  }

  @Test
  public void should_accept_exact_max_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 1000, CursorPager.Direction.NEXT);

    assertEquals(1000, param.getLimit());
  }
}
