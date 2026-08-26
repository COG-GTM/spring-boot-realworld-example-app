package io.spring.application;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
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

  private List<TestNode> nodes() {
    return Arrays.asList(new TestNode(new DateTime(1000L)), new TestNode(new DateTime(2000L)));
  }

  @Test
  public void should_only_have_next_when_paging_forward() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.NEXT, true);
    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_only_have_previous_when_paging_backward() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.PREV, true);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_expose_start_and_end_cursors() {
    CursorPager<TestNode> pager = new CursorPager<>(nodes(), Direction.NEXT, false);
    assertThat(pager.getStartCursor().toString(), is("1000"));
    assertThat(pager.getEndCursor().toString(), is("2000"));
  }

  @Test
  public void should_have_no_cursors_when_empty() {
    CursorPager<TestNode> pager = new CursorPager<>(emptyList(), Direction.NEXT, false);
    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
  }
}
