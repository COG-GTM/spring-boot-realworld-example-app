package io.spring.application.pagination;

import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_default_values() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>();

    Assertions.assertEquals(20, param.getLimit());
    Assertions.assertNull(param.getCursor());
    Assertions.assertNull(param.getDirection());
  }

  @Test
  public void should_create_with_custom_values() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 50, Direction.NEXT);

    Assertions.assertEquals(50, param.getLimit());
    Assertions.assertEquals(cursor, param.getCursor());
    Assertions.assertEquals(Direction.NEXT, param.getDirection());
  }

  @Test
  public void should_cap_limit_at_max_value() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 2000, Direction.NEXT);

    Assertions.assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_keep_limit_when_below_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 500, Direction.NEXT);

    Assertions.assertEquals(500, param.getLimit());
  }

  @Test
  public void should_keep_default_limit_when_zero_or_negative() {
    CursorPageParameter<DateTime> param1 = new CursorPageParameter<>(null, 0, Direction.NEXT);
    CursorPageParameter<DateTime> param2 = new CursorPageParameter<>(null, -10, Direction.NEXT);

    Assertions.assertEquals(20, param1.getLimit());
    Assertions.assertEquals(20, param2.getLimit());
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 50, Direction.NEXT);

    Assertions.assertEquals(51, param.getQueryLimit());
  }

  @Test
  public void should_return_true_for_is_next_when_direction_is_next() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.NEXT);

    Assertions.assertTrue(param.isNext());
  }

  @Test
  public void should_return_false_for_is_next_when_direction_is_prev() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.PREV);

    Assertions.assertFalse(param.isNext());
  }

  @Test
  public void should_handle_string_cursor() {
    CursorPageParameter<String> param = new CursorPageParameter<>("test-cursor", 30, Direction.NEXT);

    Assertions.assertEquals("test-cursor", param.getCursor());
    Assertions.assertEquals(30, param.getLimit());
  }

  @Test
  public void should_handle_null_cursor() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 20, Direction.NEXT);

    Assertions.assertNull(param.getCursor());
  }

  @Test
  public void should_handle_limit_at_exact_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 1000, Direction.NEXT);

    Assertions.assertEquals(1000, param.getLimit());
  }

  @Test
  public void should_handle_limit_just_above_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 1001, Direction.NEXT);

    Assertions.assertEquals(1000, param.getLimit());
  }
}
