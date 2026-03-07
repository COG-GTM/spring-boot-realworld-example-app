package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_indicate_has_next_when_direction_is_next_and_has_extra() {
    List<ArticleData> data = createArticleDataList(2);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, true);

    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_not_indicate_has_next_when_direction_is_next_and_no_extra() {
    List<ArticleData> data = createArticleDataList(2);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_indicate_has_previous_when_direction_is_prev_and_has_extra() {
    List<ArticleData> data = createArticleDataList(2);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.PREV, true);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_not_indicate_has_previous_when_direction_is_prev_and_no_extra() {
    List<ArticleData> data = createArticleDataList(2);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.PREV, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_start_cursor_from_first_element() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);

    assertThat(pager.getStartCursor(), notNullValue());
    assertThat(pager.getStartCursor().toString(), is(data.get(0).getCursor().toString()));
  }

  @Test
  public void should_return_end_cursor_from_last_element() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);

    assertThat(pager.getEndCursor(), notNullValue());
    assertThat(pager.getEndCursor().toString(), is(data.get(2).getCursor().toString()));
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    List<ArticleData> data = new ArrayList<>();
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);

    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
  }

  @Test
  public void should_return_data_list() {
    List<ArticleData> data = createArticleDataList(2);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);

    assertThat(pager.getData().size(), is(2));
  }

  private List<ArticleData> createArticleDataList(int count) {
    List<ArticleData> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      DateTime time = new DateTime().plusMinutes(i);
      ArticleData articleData =
          new ArticleData(
              "id-" + i,
              "slug-" + i,
              "title-" + i,
              "desc-" + i,
              "body-" + i,
              false,
              0,
              time,
              time,
              Arrays.asList("tag"),
              new ProfileData("user-" + i, "username-" + i, "bio", "image", false));
      list.add(articleData);
    }
    return list;
  }
}
