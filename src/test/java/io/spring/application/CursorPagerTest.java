package io.spring.application;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class TestNode implements Node {
    private final DateTime time;

    TestNode(DateTime time) {
      this.time = time;
    }

    @Override
    public DateTimeCursor getCursor() {
      return new DateTimeCursor(time);
    }
  }

  private final TestNode first = new TestNode(new DateTime(1000L));
  private final TestNode last = new TestNode(new DateTime(2000L));
  private final List<TestNode> nodes = Arrays.asList(first, last);

  @Test
  public void should_only_have_next_page_for_next_direction() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes, Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_only_have_previous_page_for_prev_direction() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes, Direction.PREV, true);

    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_expose_start_and_end_cursors() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes, Direction.NEXT, false);

    assertEquals("1000", pager.getStartCursor().toString());
    assertEquals("2000", pager.getEndCursor().toString());
  }

  @Test
  public void should_have_no_cursors_when_empty() {
    CursorPager<TestNode> pager = new CursorPager<>(emptyList(), Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }
}
