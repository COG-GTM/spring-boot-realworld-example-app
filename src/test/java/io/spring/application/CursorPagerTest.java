package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_indicate_next_page_when_has_extra_in_next_direction() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode()), CursorPager.Direction.NEXT, true);
    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_not_indicate_next_page_when_no_extra_in_next_direction() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode()), CursorPager.Direction.NEXT, false);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_indicate_previous_page_when_has_extra_in_prev_direction() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode()), CursorPager.Direction.PREV, true);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_not_indicate_previous_page_when_no_extra_in_prev_direction() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode()), CursorPager.Direction.PREV, false);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.emptyList(), CursorPager.Direction.NEXT, false);
    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
  }

  @Test
  public void should_return_non_null_cursors_for_non_empty_data() {
    TestNode node = new TestNode();
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(node), CursorPager.Direction.NEXT, false);
    assertThat(pager.getStartCursor(), notNullValue());
    assertThat(pager.getEndCursor(), notNullValue());
  }

  @Test
  public void should_return_data_list() {
    TestNode node1 = new TestNode();
    TestNode node2 = new TestNode();
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(node1, node2), CursorPager.Direction.NEXT, false);
    assertThat(pager.getData().size(), is(2));
  }

  private static class TestNode implements Node {
    private final DateTimeCursor cursor = new DateTimeCursor(new DateTime());

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }
}
