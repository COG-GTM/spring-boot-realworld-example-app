package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    private TestNode(long millis) {
      this.cursor = new DateTimeCursor(new DateTime(millis));
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }

  @Test
  public void should_have_next_when_next_page_has_extra_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Collections.singletonList(new TestNode(1)), CursorPager.Direction.NEXT, true);

    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_no_navigation_when_next_page_has_no_extra_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Collections.singletonList(new TestNode(1)), CursorPager.Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_previous_when_previous_page_has_extra_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Collections.singletonList(new TestNode(1)), CursorPager.Direction.PREV, true);

    assertThat(pager.hasPrevious(), is(true));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_have_null_cursors_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.emptyList(), CursorPager.Direction.NEXT, false);

    assertThat(pager.getStartCursor(), is((PageCursor) null));
    assertThat(pager.getEndCursor(), is((PageCursor) null));
  }

  @Test
  public void should_get_first_and_last_cursors() {
    List<TestNode> nodes = Arrays.asList(new TestNode(1), new TestNode(2));
    CursorPager<TestNode> pager = new CursorPager<>(nodes, CursorPager.Direction.NEXT, false);

    assertThat(pager.getStartCursor(), is(nodes.get(0).getCursor()));
    assertThat(pager.getEndCursor(), is(nodes.get(1).getCursor()));
  }
}
