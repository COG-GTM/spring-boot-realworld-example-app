package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_have_next_page_when_next_has_extra_data() {
    CursorPager<Node> pager =
        new CursorPager<>(Collections.singletonList(node(1)), Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_have_no_pages_when_next_has_no_extra_data() {
    CursorPager<Node> pager =
        new CursorPager<>(Collections.singletonList(node(1)), Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_have_previous_page_when_previous_has_extra_data() {
    CursorPager<Node> pager =
        new CursorPager<>(Collections.singletonList(node(1)), Direction.PREV, true);

    assertTrue(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<Node> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_first_and_last_cursors() {
    Node first = node(1);
    Node last = node(2);
    CursorPager<Node> pager = new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, false);

    assertEquals(first.getCursor(), pager.getStartCursor());
    assertEquals(last.getCursor(), pager.getEndCursor());
  }

  private static Node node(long millis) {
    return new Node(new DateTimeCursor(new DateTime(millis)));
  }

  private static class Node implements io.spring.application.Node {
    private final DateTimeCursor cursor;

    private Node(DateTimeCursor cursor) {
      this.cursor = cursor;
    }

    @Override
    public DateTimeCursor getCursor() {
      return cursor;
    }
  }
}
