package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_create_pager_with_next_direction_and_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.getData(), is(data));
    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_create_pager_with_next_direction_without_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_create_pager_with_prev_direction_and_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_create_pager_with_prev_direction_without_extra() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_start_cursor_from_first_element() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getStartCursor(), is(data.get(0).getCursor()));
  }

  @Test
  public void should_return_end_cursor_from_last_element() {
    List<TestNode> data = createTestNodes(3);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getEndCursor(), is(data.get(2).getCursor()));
  }

  @Test
  public void should_return_null_start_cursor_for_empty_data() {
    List<TestNode> data = new ArrayList<>();
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getStartCursor(), nullValue());
  }

  @Test
  public void should_return_null_end_cursor_for_empty_data() {
    List<TestNode> data = new ArrayList<>();
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getEndCursor(), nullValue());
  }

  @Test
  public void should_return_same_cursor_for_single_element() {
    List<TestNode> data = createTestNodes(1);
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getStartCursor(), is(pager.getEndCursor()));
  }

  private List<TestNode> createTestNodes(int count) {
    List<TestNode> nodes = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      nodes.add(new TestNode(new DateTime().plusDays(i)));
    }
    return nodes;
  }

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    public TestNode(DateTime dateTime) {
      this.cursor = new DateTimeCursor(dateTime);
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }
}
