package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
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
  public void should_report_next_and_no_previous_for_next_direction() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()), new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);
    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
    assertThat(pager.getData(), is(data));
  }

  @Test
  public void should_report_previous_and_no_next_for_prev_direction() {
    List<TestNode> data = Collections.singletonList(new TestNode(new DateTime()));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);
    assertThat(pager.hasPrevious(), is(true));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_return_start_and_end_cursor_from_data() {
    DateTime first = new DateTime(2020, 1, 1, 0, 0);
    DateTime last = new DateTime(2020, 12, 31, 0, 0);
    List<TestNode> data = Arrays.asList(new TestNode(first), new TestNode(last));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);
    assertThat(pager.getStartCursor().getData(), is(first));
    assertThat(pager.getEndCursor().getData(), is(last));
  }

  @Test
  public void should_return_null_cursors_when_data_empty() {
    CursorPager<TestNode> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    assertThat(pager.getStartCursor(), is(nullValue()));
    assertThat(pager.getEndCursor(), is(nullValue()));
  }
}
