package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  void should_have_next_when_direction_is_next_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()), new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_not_have_next_when_direction_is_next_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_have_previous_when_direction_is_prev_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()), new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  void should_not_have_previous_when_direction_is_prev_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_return_null_cursors_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  void should_return_start_and_end_cursors_for_non_empty_data() {
    DateTime first = new DateTime(1000);
    DateTime second = new DateTime(2000);
    List<TestNode> data = Arrays.asList(new TestNode(first), new TestNode(second));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
    assertEquals(first, ((DateTimeCursor) pager.getStartCursor()).getData());
    assertEquals(second, ((DateTimeCursor) pager.getEndCursor()).getData());
  }

  @Test
  void should_return_same_cursor_for_single_element() {
    DateTime time = new DateTime(5000);
    List<TestNode> data = Collections.singletonList(new TestNode(time));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals(
        ((DateTimeCursor) pager.getStartCursor()).getData(),
        ((DateTimeCursor) pager.getEndCursor()).getData());
  }

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    TestNode(DateTime dateTime) {
      this.cursor = new DateTimeCursor(dateTime);
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }
}
