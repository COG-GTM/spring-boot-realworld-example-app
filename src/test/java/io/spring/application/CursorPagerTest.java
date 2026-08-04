package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

  private static final TestNode FIRST =
      new TestNode(new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC));
  private static final TestNode MIDDLE =
      new TestNode(new DateTime(2020, 2, 1, 0, 0, DateTimeZone.UTC));
  private static final TestNode LAST =
      new TestNode(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC));

  private static List<TestNode> nodes() {
    return Arrays.asList(FIRST, MIDDLE, LAST);
  }

  @Test
  public void should_only_have_next_when_paging_forward_with_extra() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.NEXT, true);

    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_no_next_when_paging_forward_without_extra() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_only_have_previous_when_paging_backward_with_extra() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.PREV, true);

    assertThat(pager.hasPrevious(), is(true));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_have_no_previous_when_paging_backward_without_extra() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.PREV, false);

    assertThat(pager.hasPrevious(), is(false));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_expose_data_and_boundary_cursors() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.NEXT, true);

    assertThat(pager.getData(), is(nodes()));
    assertThat(pager.getStartCursor(), sameInstance(FIRST.getCursor()));
    assertThat(pager.getEndCursor(), sameInstance(LAST.getCursor()));
  }

  @Test
  public void should_use_single_element_as_both_cursors() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.singletonList(MIDDLE), Direction.NEXT, false);

    assertThat(pager.getStartCursor(), sameInstance(MIDDLE.getCursor()));
    assertThat(pager.getEndCursor(), sameInstance(MIDDLE.getCursor()));
  }

  @Test
  public void should_return_null_cursors_when_empty_going_next() {
    CursorPager<TestNode> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_null_cursors_when_empty_going_prev() {
    CursorPager<TestNode> pager = new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }
}
