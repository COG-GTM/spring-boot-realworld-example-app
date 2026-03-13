package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  static class TestNode implements Node {
    private final PageCursor cursor;

    TestNode(String cursorValue) {
      this.cursor =
          new PageCursor<String>(cursorValue) {
            @Override
            public String toString() {
              return getData();
            }
          };
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }

  @Test
  public void should_set_next_true_when_direction_next_and_has_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode("a")), CursorPager.Direction.NEXT, true);
    assertThat(pager.hasPrevious(), is(false));
    assertThat(pager.hasNext(), is(true));
  }

  @Test
  public void should_set_next_false_when_direction_next_and_no_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode("a")), CursorPager.Direction.NEXT, false);
    assertThat(pager.hasPrevious(), is(false));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_set_previous_true_when_direction_prev_and_has_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode("a")), CursorPager.Direction.PREV, true);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_set_previous_false_when_direction_prev_and_no_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(new TestNode("a")), CursorPager.Direction.PREV, false);
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_null_start_cursor_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    assertThat(pager.getStartCursor(), is(nullValue()));
  }

  @Test
  public void should_return_first_element_cursor_as_start_cursor() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Arrays.asList(new TestNode("first"), new TestNode("last")),
            CursorPager.Direction.NEXT,
            false);
    assertThat(pager.getStartCursor().toString(), is("first"));
  }

  @Test
  public void should_return_null_end_cursor_for_empty_data() {
    CursorPager<TestNode> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    assertThat(pager.getEndCursor(), is(nullValue()));
  }

  @Test
  public void should_return_last_element_cursor_as_end_cursor() {
    CursorPager<TestNode> pager =
        new CursorPager<>(
            Arrays.asList(new TestNode("first"), new TestNode("last")),
            CursorPager.Direction.NEXT,
            false);
    assertThat(pager.getEndCursor().toString(), is("last"));
  }
}
