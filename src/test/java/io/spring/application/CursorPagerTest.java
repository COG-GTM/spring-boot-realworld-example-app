package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private CommentData comment(String id, DateTime createdAt) {
    return new CommentData(id, "body", "articleId", createdAt, createdAt, null);
  }

  @Test
  public void should_mark_extra_data_as_next_page_when_paging_forward() {
    CommentData first = comment("1", new DateTime(1000L));
    CommentData last = comment("2", new DateTime(2000L));

    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, true);

    assertThat(pager.getData()).containsExactly(first, last);
    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.getStartCursor().getData()).isEqualTo(first.getCreatedAt());
    assertThat(pager.getEndCursor().getData()).isEqualTo(last.getCreatedAt());
  }

  @Test
  public void should_mark_extra_data_as_previous_page_when_paging_backward() {
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(comment("1", new DateTime(1000L))), Direction.PREV, true);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isTrue();
  }

  @Test
  public void should_have_no_cursors_when_data_is_empty() {
    List<CommentData> empty = new ArrayList<>();

    CursorPager<CommentData> pager = new CursorPager<>(empty, Direction.NEXT, false);

    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isFalse();
  }
}
