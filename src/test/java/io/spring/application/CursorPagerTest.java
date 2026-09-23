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
    private final DateTime cursor;

    TestNode(DateTime cursor) {
      this.cursor = cursor;
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(cursor);
    }
  }

  private final TestNode first = new TestNode(new DateTime(1000L));
  private final TestNode last = new TestNode(new DateTime(2000L));

  @Test
  public void should_only_have_next_when_paging_forward_with_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, true);

    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_only_have_previous_when_paging_backward_with_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.PREV, true);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_have_no_sibling_page_without_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_get_cursors_of_first_and_last_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, false);

    assertThat(pager.getStartCursor().toString(), is("1000"));
    assertThat(pager.getEndCursor().toString(), is("2000"));
  }

  @Test
  public void should_get_null_cursors_when_data_is_empty() {
    List<TestNode> empty = Collections.emptyList();
    CursorPager<TestNode> pager = new CursorPager<>(empty, Direction.NEXT, false);

    assertThat(pager.getStartCursor(), is(nullValue()));
    assertThat(pager.getEndCursor(), is(nullValue()));
  }
}
