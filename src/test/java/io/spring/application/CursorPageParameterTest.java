package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void clamps_page_size_to_max_limit() {
    CursorPageParameter<String> page = new CursorPageParameter<>(null, 1000, Direction.NEXT);

    assertEquals(100, page.getLimit());
    assertEquals(101, page.getQueryLimit());
  }

  @Test
  public void keeps_page_size_within_limit() {
    assertEquals(50, new CursorPageParameter<>(null, 50, Direction.NEXT).getLimit());
    assertEquals(20, new CursorPageParameter<>(null, 0, Direction.NEXT).getLimit());
  }
}
