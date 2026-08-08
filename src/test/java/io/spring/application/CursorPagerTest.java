package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private ArticleData articleUpdatedAt(long millis) {
    ArticleData articleData = new ArticleData();
    articleData.setUpdatedAt(new DateTime(millis));
    return articleData;
  }

  @Test
  public void should_report_next_page_for_forward_direction() {
    ArticleData first = articleUpdatedAt(1000L);
    ArticleData last = articleUpdatedAt(3000L);

    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(first, last), Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.getData()).containsExactly(first, last);
    assertThat(pager.getStartCursor().toString()).isEqualTo("1000");
    assertThat(pager.getEndCursor().toString()).isEqualTo("3000");
  }

  @Test
  public void should_report_previous_page_for_backward_direction() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(articleUpdatedAt(2000L)), Direction.PREV, true);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isTrue();
    assertThat(pager.getStartCursor().toString()).isEqualTo("2000");
    assertThat(pager.getEndCursor().toString()).isEqualTo("2000");
  }

  @Test
  public void should_not_report_extra_page_when_no_extra_row() {
    CursorPager<ArticleData> next =
        new CursorPager<>(Arrays.asList(articleUpdatedAt(1L)), Direction.NEXT, false);
    CursorPager<ArticleData> prev =
        new CursorPager<>(Arrays.asList(articleUpdatedAt(1L)), Direction.PREV, false);

    assertThat(next.hasNext()).isFalse();
    assertThat(next.hasPrevious()).isFalse();
    assertThat(prev.hasNext()).isFalse();
    assertThat(prev.hasPrevious()).isFalse();
  }

  @Test
  public void should_return_null_cursors_for_empty_page() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getData()).isEmpty();
    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
  }
}
