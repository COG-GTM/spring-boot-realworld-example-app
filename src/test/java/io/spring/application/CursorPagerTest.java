package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.util.ArrayList;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  public void should_set_next_when_direction_is_next_and_has_extra() {
    ArticleData data = buildArticleData("1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data), Direction.NEXT, true);
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_not_set_next_when_direction_is_next_and_no_extra() {
    ArticleData data = buildArticleData("1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data), Direction.NEXT, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_set_previous_when_direction_is_prev_and_has_extra() {
    ArticleData data = buildArticleData("1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data), Direction.PREV, true);
    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_not_set_previous_when_direction_is_prev_and_no_extra() {
    ArticleData data = buildArticleData("1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data), Direction.PREV, false);
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_null_cursors_when_empty() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_cursors_when_data_present() {
    ArticleData data1 = buildArticleData("1");
    ArticleData data2 = buildArticleData("2");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data1, data2), Direction.NEXT, false);
    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
  }

  @Test
  public void should_get_data() {
    ArticleData data = buildArticleData("1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(data), Direction.NEXT, false);
    assertEquals(1, pager.getData().size());
  }

  private ArticleData buildArticleData(String id) {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("user-id", "user", "", "", false);
    return new ArticleData(
        id, "slug-" + id, "Title", "Desc", "Body", false, 0, now, now, new ArrayList<>(), profile);
  }
}
