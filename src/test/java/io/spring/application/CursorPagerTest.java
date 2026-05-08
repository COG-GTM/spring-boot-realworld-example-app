package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

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

  @Test
  public void should_indicate_has_next_when_direction_is_next_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, true);
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_indicate_no_next_when_direction_is_next_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_indicate_has_previous_when_direction_is_prev_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.PREV, true);
    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_indicate_no_previous_when_direction_is_prev_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.PREV, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_start_cursor_from_first_element() {
    DateTime dt = new DateTime();
    List<TestNode> data = Arrays.asList(new TestNode(dt), new TestNode(dt.plusHours(1)));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertNotNull(pager.getStartCursor());
    assertEquals(String.valueOf(dt.getMillis()), pager.getStartCursor().toString());
  }

  @Test
  public void should_return_end_cursor_from_last_element() {
    DateTime dt1 = new DateTime();
    DateTime dt2 = dt1.plusHours(1);
    List<TestNode> data = Arrays.asList(new TestNode(dt1), new TestNode(dt2));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertNotNull(pager.getEndCursor());
    assertEquals(String.valueOf(dt2.getMillis()), pager.getEndCursor().toString());
  }

  @Test
  public void should_return_data() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertEquals(1, pager.getData().size());
  }
}
