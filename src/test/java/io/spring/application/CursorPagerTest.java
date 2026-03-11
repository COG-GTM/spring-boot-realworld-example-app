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
  public void should_set_next_true_when_direction_is_next_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_set_next_false_when_direction_is_next_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_set_previous_true_when_direction_is_prev_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_set_previous_false_when_direction_is_prev_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_null_start_cursor_for_empty_data() {
    List<TestNode> data = Collections.emptyList();
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertNull(pager.getStartCursor());
  }

  @Test
  public void should_return_null_end_cursor_for_empty_data() {
    List<TestNode> data = Collections.emptyList();
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_first_element_cursor_as_start_cursor() {
    DateTime time1 = new DateTime(2023, 1, 1, 0, 0);
    DateTime time2 = new DateTime(2023, 1, 2, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(time1), new TestNode(time2));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals(time1.getMillis(), ((DateTimeCursor) pager.getStartCursor()).getData().getMillis());
  }

  @Test
  public void should_return_last_element_cursor_as_end_cursor() {
    DateTime time1 = new DateTime(2023, 1, 1, 0, 0);
    DateTime time2 = new DateTime(2023, 1, 2, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(time1), new TestNode(time2));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals(time2.getMillis(), ((DateTimeCursor) pager.getEndCursor()).getData().getMillis());
  }

  @Test
  public void should_return_data_list() {
    DateTime time = new DateTime();
    List<TestNode> data = Arrays.asList(new TestNode(time));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals(1, pager.getData().size());
    assertEquals(data, pager.getData());
  }

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    public TestNode(DateTime time) {
      this.cursor = new DateTimeCursor(time);
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }
}
