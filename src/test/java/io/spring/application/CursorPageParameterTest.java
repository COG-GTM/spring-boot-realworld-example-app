package io.spring.application;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_keep_valid_limit_and_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    Assertions.assertEquals(10, parameter.getLimit());
    Assertions.assertEquals(11, parameter.getQueryLimit());
    Assertions.assertEquals(cursor, parameter.getCursor());
    Assertions.assertTrue(parameter.isNext());
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(null, 5000, Direction.PREV);

    Assertions.assertEquals(1000, parameter.getLimit());
    Assertions.assertFalse(parameter.isNext());
  }

  @Test
  public void should_use_default_limit_for_non_positive_values() {
    Assertions.assertEquals(20, new CursorPageParameter<>(null, 0, Direction.NEXT).getLimit());
    Assertions.assertEquals(20, new CursorPageParameter<>(null, -1, Direction.NEXT).getLimit());
  }
}
