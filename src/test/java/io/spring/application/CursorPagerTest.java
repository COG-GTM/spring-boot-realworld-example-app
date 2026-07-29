package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static final DateTime EPOCH = new DateTime(0L, DateTimeZone.UTC);

  /** Minimal {@link Node} whose cursor is derived from a fixed offset in milliseconds. */
  private static class TestNode implements Node {
    private final long millis;

    TestNode(long millis) {
      this.millis = millis;
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(EPOCH.withMillis(millis));
    }

    @Override
    public String toString() {
      return "TestNode(" + millis + ")";
    }
  }

  private static List<TestNode> nodes(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new TestNode(i + 1))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static List<Long> millisOf(List<TestNode> data) {
    return data.stream().map(node -> node.millis).collect(Collectors.toList());
  }

  /**
   * Mirrors what the query services do with the rows returned for {@code getQueryLimit()}: drop the
   * extra probe row and, for backward paging, restore ascending order.
   */
  private static CursorPager<TestNode> page(
      List<TestNode> fetched, CursorPageParameter<DateTime> parameter) {
    List<TestNode> data = new ArrayList<>(fetched);
    boolean hasExtra = data.size() > parameter.getLimit();
    if (hasExtra) {
      data.remove(parameter.getLimit());
    }
    if (!parameter.isNext()) {
      Collections.reverse(data);
    }
    return new CursorPager<>(data, parameter.getDirection(), hasExtra);
  }

  @Test
  public void should_have_next_and_no_previous_when_paging_forward_with_extra_row() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(3), Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.isNext()).isTrue();
    assertThat(pager.isPrevious()).isFalse();
  }

  @Test
  public void should_have_no_next_when_paging_forward_without_extra_row() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(3), Direction.NEXT, false);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void should_have_previous_and_no_next_when_paging_backward_with_extra_row() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(3), Direction.PREV, true);

    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_have_no_previous_when_paging_backward_without_extra_row() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(3), Direction.PREV, false);

    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_expose_the_data_it_was_built_with() {
    List<TestNode> data = nodes(3);

    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getData()).containsExactlyElementsOf(data);
  }

  @Test
  public void should_return_null_cursors_for_an_empty_page() {
    CursorPager<TestNode> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    assertThat(pager.getData()).isEmpty();
    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void should_take_cursors_from_the_first_and_last_node() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(3), Direction.NEXT, false);

    assertThat(pager.getStartCursor().toString()).isEqualTo("1");
    assertThat(pager.getEndCursor().toString()).isEqualTo("3");
  }

  @Test
  public void should_use_the_same_cursor_for_start_and_end_of_a_single_element_page() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(1), Direction.PREV, false);

    assertThat(pager.getStartCursor().toString()).isEqualTo("1");
    assertThat(pager.getEndCursor().toString()).isEqualTo("1");
  }

  @Test
  public void should_trim_the_probe_row_and_report_next_when_paging_forward() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(EPOCH, 2, Direction.NEXT);

    // the read services fetch getQueryLimit() (= limit + 1) rows to detect a further page
    CursorPager<TestNode> pager = page(nodes(parameter.getQueryLimit()), parameter);

    assertThat(millisOf(pager.getData())).containsExactly(1L, 2L);
    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.getStartCursor().toString()).isEqualTo("1");
    assertThat(pager.getEndCursor().toString()).isEqualTo("2");
  }

  @Test
  public void should_keep_all_rows_when_the_last_forward_page_is_not_full() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(EPOCH, 5, Direction.NEXT);

    CursorPager<TestNode> pager = page(nodes(3), parameter);

    assertThat(millisOf(pager.getData())).containsExactly(1L, 2L, 3L);
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_keep_all_rows_when_the_forward_page_is_exactly_full() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(EPOCH, 3, Direction.NEXT);

    CursorPager<TestNode> pager = page(nodes(3), parameter);

    assertThat(millisOf(pager.getData())).containsExactly(1L, 2L, 3L);
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_trim_the_probe_row_and_reverse_when_paging_backward() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(EPOCH, 2, Direction.PREV);

    // backward reads come back newest-first: 3, 2, 1 (1 being the probe row)
    List<TestNode> fetched = nodes(parameter.getQueryLimit());
    Collections.reverse(fetched);

    CursorPager<TestNode> pager = page(fetched, parameter);

    assertThat(millisOf(pager.getData())).containsExactly(2L, 3L);
    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.getStartCursor().toString()).isEqualTo("2");
    assertThat(pager.getEndCursor().toString()).isEqualTo("3");
  }

  @Test
  public void should_report_no_previous_when_the_backward_page_reaches_the_beginning() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(EPOCH, 5, Direction.PREV);

    List<TestNode> fetched = nodes(2);
    Collections.reverse(fetched);

    CursorPager<TestNode> pager = page(fetched, parameter);

    assertThat(millisOf(pager.getData())).containsExactly(1L, 2L);
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.hasNext()).isFalse();
  }
}
