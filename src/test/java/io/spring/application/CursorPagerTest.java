package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
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
    public PageCursor getCursor() {
      return new DateTimeCursor(time);
    }
  }

  @Test
  public void should_have_next_when_direction_is_next_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, true);
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_not_have_next_when_direction_is_next_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_have_previous_when_direction_is_prev_and_has_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.PREV, true);
    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_not_have_previous_when_direction_is_prev_and_no_extra() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.PREV, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_start_cursor() {
    DateTime time = new DateTime(2023, 1, 1, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(time));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertNotNull(pager.getStartCursor());
    assertEquals(String.valueOf(time.getMillis()), pager.getStartCursor().toString());
  }

  @Test
  public void should_return_end_cursor() {
    DateTime time1 = new DateTime(2023, 1, 1, 0, 0);
    DateTime time2 = new DateTime(2023, 6, 1, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(time1), new TestNode(time2));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertNotNull(pager.getEndCursor());
    assertEquals(String.valueOf(time2.getMillis()), pager.getEndCursor().toString());
  }

  @Test
  public void should_return_null_cursor_for_empty_data() {
    CursorPager<TestNode> pager = new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_data() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()), new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    assertEquals(2, pager.getData().size());
  }

  @Test
  public void should_have_direction_enum_values() {
    assertEquals(2, CursorPager.Direction.values().length);
    assertNotNull(CursorPager.Direction.valueOf("NEXT"));
    assertNotNull(CursorPager.Direction.valueOf("PREV"));
  }
}
