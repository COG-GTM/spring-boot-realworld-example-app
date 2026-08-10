package io.spring.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Node;
import io.spring.application.PageCursor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class CursorPagerTest {

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

  private final DateTime first = new DateTime(1000L);
  private final DateTime last = new DateTime(5000L);

  @Test
  void should_report_next_only_when_paging_forward() {
    List<TestNode> data = Arrays.asList(new TestNode(first), new TestNode(last));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.isNext()).isTrue();
    assertThat(pager.isPrevious()).isFalse();
    assertThat(pager.getData()).isEqualTo(data);
  }

  @Test
  void should_report_previous_only_when_paging_backward() {
    List<TestNode> data = Collections.singletonList(new TestNode(first));

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, true);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.isNext()).isFalse();
    assertThat(pager.isPrevious()).isTrue();
  }

  @Test
  void should_render_raw_cursor_data_by_default() {
    PageCursor<String> cursor = new PageCursor<String>("raw") {};

    assertThat(cursor.getData()).isEqualTo("raw");
    assertThat(cursor.toString()).isEqualTo("raw");
  }

  @Test
  void should_report_no_extra_page_when_has_extra_is_false() {
    CursorPager<TestNode> next =
        new CursorPager<>(Collections.singletonList(new TestNode(first)), Direction.NEXT, false);
    CursorPager<TestNode> prev =
        new CursorPager<>(Collections.singletonList(new TestNode(first)), Direction.PREV, false);

    assertThat(next.hasNext()).isFalse();
    assertThat(next.hasPrevious()).isFalse();
    assertThat(prev.hasNext()).isFalse();
    assertThat(prev.hasPrevious()).isFalse();
  }

  @Test
  void should_expose_first_and_last_element_cursors() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Arrays.asList(new TestNode(first), new TestNode(last)), Direction.NEXT, false);

    assertThat(pager.getStartCursor().toString()).isEqualTo("1000");
    assertThat(pager.getEndCursor().toString()).isEqualTo("5000");
  }

  @Test
  void should_return_null_cursors_when_empty() {
    CursorPager<TestNode> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
  }

  @Test
  void should_expose_both_directions() {
    assertThat(Direction.valueOf("NEXT")).isEqualTo(Direction.NEXT);
    assertThat(Direction.values()).containsExactly(Direction.PREV, Direction.NEXT);
  }
}
