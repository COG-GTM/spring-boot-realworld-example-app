package io.spring.application;

import static io.spring.application.CursorPager.Direction.NEXT;
import static io.spring.application.CursorPager.Direction.PREV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_default_limit() {
    assertEquals(20, new CursorPageParameter<>().getLimit());
  }

  @Test
  public void should_cap_limit_at_maximum() {
    assertEquals(1000, new CursorPageParameter<>(null, 5000, NEXT).getLimit());
  }

  @Test
  public void should_keep_default_limit_for_non_positive_values() {
    assertEquals(20, new CursorPageParameter<>(null, 0, NEXT).getLimit());
    assertEquals(20, new CursorPageParameter<>(null, -1, NEXT).getLimit());
  }

  @Test
  public void should_add_one_to_query_limit() {
    assertEquals(11, new CursorPageParameter<>(null, 10, NEXT).getQueryLimit());
  }

  @Test
  public void should_detect_next_direction() {
    assertTrue(new CursorPageParameter<>(null, 20, NEXT).isNext());
    assertFalse(new CursorPageParameter<>(null, 20, PREV).isNext());
  }

  @Test
  public void should_store_cursor() {
    String cursor = "cursor";

    assertEquals(cursor, new CursorPageParameter<>(cursor, 20, NEXT).getCursor());
  }
}
