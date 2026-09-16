package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class TestNode implements Node {
    private final DateTime time;

    TestNode(long millis) {
      this.time = new DateTime(millis);
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(time);
    }
  }

  @Test
  public void should_have_next_when_paging_forward_with_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode(1), new TestNode(2)), Direction.NEXT, true);
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_not_have_next_when_paging_forward_without_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode(1)), Direction.NEXT, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_have_previous_when_paging_backward_with_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode(1)), Direction.PREV, true);
    assertTrue(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_not_have_previous_when_paging_backward_without_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode(1)), Direction.PREV, false);
    assertFalse(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_expose_start_and_end_cursors() {
    List<TestNode> data = Arrays.asList(new TestNode(100), new TestNode(200), new TestNode(300));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);
    assertEquals("100", pager.getStartCursor().toString());
    assertEquals("300", pager.getEndCursor().toString());
    assertEquals(data, pager.getData());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<TestNode> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }
}
