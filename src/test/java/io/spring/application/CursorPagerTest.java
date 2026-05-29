package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    TestNode(DateTime time) {
      this.cursor = new DateTimeCursor(time);
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }

  @Test
  void should_have_next_when_direction_next_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, true);
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_have_previous_when_direction_prev_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.PREV, true);
    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  void should_not_have_next_or_previous_when_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_return_null_cursors_when_empty() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.emptyList(), CursorPager.Direction.NEXT, false);
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  void should_return_start_and_end_cursors() {
    DateTime time1 = new DateTime(2022, 1, 1, 0, 0);
    DateTime time2 = new DateTime(2022, 1, 2, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(time1), new TestNode(time2));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
    assertEquals(data.get(0).getCursor(), pager.getStartCursor());
    assertEquals(data.get(1).getCursor(), pager.getEndCursor());
  }

  @Test
  void should_return_data() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertEquals(1, pager.getData().size());
  }
}
