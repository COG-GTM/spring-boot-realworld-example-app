package io.spring.application.pagination;

import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Node;
import io.spring.application.PageCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_create_cursor_pager_with_next_direction_and_has_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    Assertions.assertEquals(3, pager.getData().size());
    Assertions.assertTrue(pager.hasNext());
    Assertions.assertFalse(pager.hasPrevious());
    Assertions.assertTrue(pager.isNext());
    Assertions.assertFalse(pager.isPrevious());
  }

  @Test
  public void should_create_cursor_pager_with_next_direction_and_no_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    Assertions.assertEquals(3, pager.getData().size());
    Assertions.assertFalse(pager.hasNext());
    Assertions.assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_create_cursor_pager_with_prev_direction_and_has_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    Assertions.assertEquals(3, pager.getData().size());
    Assertions.assertFalse(pager.hasNext());
    Assertions.assertTrue(pager.hasPrevious());
    Assertions.assertFalse(pager.isNext());
    Assertions.assertTrue(pager.isPrevious());
  }

  @Test
  public void should_create_cursor_pager_with_prev_direction_and_no_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    Assertions.assertEquals(3, pager.getData().size());
    Assertions.assertFalse(pager.hasNext());
    Assertions.assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_start_cursor_from_first_element() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    PageCursor startCursor = pager.getStartCursor();
    Assertions.assertNotNull(startCursor);
    Assertions.assertEquals(data.get(0).getCursor().toString(), startCursor.toString());
  }

  @Test
  public void should_return_end_cursor_from_last_element() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    PageCursor endCursor = pager.getEndCursor();
    Assertions.assertNotNull(endCursor);
    Assertions.assertEquals(data.get(2).getCursor().toString(), endCursor.toString());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    List<TestNode> data = new ArrayList<>();
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    Assertions.assertNull(pager.getStartCursor());
    Assertions.assertNull(pager.getEndCursor());
  }

  @Test
  public void should_handle_single_element_data() {
    List<TestNode> data = createTestNodes(1);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    Assertions.assertEquals(1, pager.getData().size());
    Assertions.assertNotNull(pager.getStartCursor());
    Assertions.assertNotNull(pager.getEndCursor());
    Assertions.assertEquals(pager.getStartCursor().toString(), pager.getEndCursor().toString());
  }

  private List<TestNode> createTestNodes(int count) {
    List<TestNode> nodes = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      nodes.add(new TestNode(new DateTime().plusMinutes(i)));
    }
    return nodes;
  }

  private static class TestNode implements Node {
    private final DateTime createdAt;

    public TestNode(DateTime createdAt) {
      this.createdAt = createdAt;
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(createdAt);
    }
  }
}
