package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class PaginationTest {

  @Test
  public void page_should_clamp_limit_and_ignore_invalid_values() {
    Page capped = new Page(5, 1000);
    assertEquals(5, capped.getOffset());
    assertEquals(100, capped.getLimit());

    Page defaults = new Page(-1, 0);
    assertEquals(0, defaults.getOffset());
    assertEquals(20, defaults.getLimit());

    Page custom = new Page(0, 30);
    assertEquals(30, custom.getLimit());
  }

  @Test
  public void cursor_page_parameter_should_clamp_limit() {
    CursorPageParameter<String> capped = new CursorPageParameter<>("c", 5000, Direction.NEXT);
    assertEquals(1000, capped.getLimit());
    assertEquals(1001, capped.getQueryLimit());
    assertTrue(capped.isNext());
    assertEquals("c", capped.getCursor());

    CursorPageParameter<String> defaults = new CursorPageParameter<>(null, -3, Direction.PREV);
    assertEquals(20, defaults.getLimit());
    assertFalse(defaults.isNext());
  }

  @Test
  public void empty_cursor_pager_should_have_no_cursors() {
    CursorPager<CommentData> next = new CursorPager<>(new ArrayList<>(), Direction.NEXT, true);
    assertTrue(next.hasNext());
    assertFalse(next.hasPrevious());
    assertNull(next.getStartCursor());
    assertNull(next.getEndCursor());

    CursorPager<CommentData> prev = new CursorPager<>(new ArrayList<>(), Direction.PREV, true);
    assertFalse(prev.hasNext());
    assertTrue(prev.hasPrevious());
  }

  @Test
  public void date_time_cursor_should_handle_null() {
    assertNull(DateTimeCursor.parse(null));
  }
}
