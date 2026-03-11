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
  public void should_have_next_when_direction_is_next_and_has_extra() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, true);
    
    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_not_have_next_when_direction_is_next_and_no_extra() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_previous_when_direction_is_prev_and_has_extra() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.PREV, true);
    
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(true));
  }

  @Test
  public void should_not_have_previous_when_direction_is_prev_and_no_extra() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.PREV, false);
    
    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_return_data_list() {
    List<ArticleData> data = createArticleDataList(3);
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    
    assertThat(pager.getData(), is(data));
    assertThat(pager.getData().size(), is(3));
  }

  @Test
  public void should_return_null_start_cursor_for_empty_data() {
    List<ArticleData> data = new ArrayList<>();
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    
    assertThat(pager.getStartCursor(), nullValue());
  }

  @Test
  public void should_return_null_end_cursor_for_empty_data() {
    List<ArticleData> data = new ArrayList<>();
    CursorPager<ArticleData> pager = new CursorPager<>(data, CursorPager.Direction.NEXT, false);
    
    assertThat(pager.getEndCursor(), nullValue());
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

  private List<ArticleData> createArticleDataList(int count) {
    List<ArticleData> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      DateTime now = new DateTime().plusMinutes(i);
      ProfileData profile = new ProfileData("id" + i, "user" + i, "bio", "image", false);
      ArticleData article = new ArticleData(
          "id" + i,
          "slug-" + i,
          "title " + i,
          "desc " + i,
          "body " + i,
          false,
          0,
          now,
          now,
          Arrays.asList("tag1"),
          profile
      );
      list.add(article);
    }
    return list;
  }
}
