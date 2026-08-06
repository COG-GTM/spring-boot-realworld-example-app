package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.TestHelper;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private CommentData first;
  private CommentData last;
  private List<CommentData> data;

  @BeforeEach
  public void setUp() {
    User user = TestHelper.userFixture("cursor");
    first = TestHelper.commentDataFixture("first", user);
    last = TestHelper.commentDataFixture("last", user);
    data = Arrays.asList(first, last);
  }

  @Test
  public void should_only_have_next_when_direction_is_next_with_extra() {
    CursorPager<CommentData> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.isNext()).isTrue();
    assertThat(pager.isPrevious()).isFalse();
    assertThat(pager.getData()).isEqualTo(data);
  }

  @Test
  public void should_have_no_next_when_direction_is_next_without_extra() {
    CursorPager<CommentData> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void should_only_have_previous_when_direction_is_prev_with_extra() {
    CursorPager<CommentData> pager = new CursorPager<>(data, Direction.PREV, true);

    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.isPrevious()).isTrue();
    assertThat(pager.isNext()).isFalse();
  }

  @Test
  public void should_have_no_previous_when_direction_is_prev_without_extra() {
    CursorPager<CommentData> pager = new CursorPager<>(data, Direction.PREV, false);

    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_return_first_and_last_cursors() {
    CursorPager<CommentData> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.getStartCursor().getData()).isEqualTo(first.getCreatedAt());
    assertThat(pager.getEndCursor().getData()).isEqualTo(last.getCreatedAt());
  }

  @Test
  public void should_return_the_same_cursor_for_a_single_element() {
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.singletonList(first), Direction.NEXT, false);

    assertThat(pager.getStartCursor().getData()).isEqualTo(first.getCreatedAt());
    assertThat(pager.getEndCursor().getData()).isEqualTo(first.getCreatedAt());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
  }
}
