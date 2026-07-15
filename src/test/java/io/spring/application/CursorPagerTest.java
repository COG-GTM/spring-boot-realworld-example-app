package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
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
  public void next_direction_should_have_next_when_extra_and_no_previous() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void next_direction_without_extra_should_not_have_next() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void prev_direction_should_have_previous_when_extra_and_no_next() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void prev_direction_without_extra_should_not_have_previous() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_expose_data_getter() {
    List<TestNode> data = Arrays.asList(new TestNode(new DateTime()), new TestNode(new DateTime()));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getData()).hasSize(2);
    assertThat(pager.getData()).isEqualTo(data);
  }

  @Test
  public void should_return_start_and_end_cursor_from_boundary_elements() {
    DateTime first = new DateTime(1000L);
    DateTime last = new DateTime(2000L);
    List<TestNode> data = Arrays.asList(new TestNode(first), new TestNode(last));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getStartCursor().getData()).isEqualTo(first);
    assertThat(pager.getEndCursor().getData()).isEqualTo(last);
  }

  @Test
  public void should_return_null_cursors_when_data_empty() {
    CursorPager<TestNode> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
  }
}
