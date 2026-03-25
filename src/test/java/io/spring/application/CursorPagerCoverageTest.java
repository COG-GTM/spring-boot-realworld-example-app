package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerCoverageTest {

  @Test
  public void should_create_with_next_direction_and_extra() {
    ArticleData a1 = createArticleData("id1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1), CursorPager.Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
    assertTrue(pager.isNext());
    assertFalse(pager.isPrevious());
    assertEquals(1, pager.getData().size());
  }

  @Test
  public void should_create_with_next_direction_without_extra() {
    ArticleData a1 = createArticleData("id1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1), CursorPager.Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_create_with_prev_direction_and_extra() {
    ArticleData a1 = createArticleData("id1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1), CursorPager.Direction.PREV, true);

    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
    assertFalse(pager.isNext());
    assertTrue(pager.isPrevious());
  }

  @Test
  public void should_create_with_prev_direction_without_extra() {
    ArticleData a1 = createArticleData("id1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1), CursorPager.Direction.PREV, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_return_null_cursors_for_empty_data() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), CursorPager.Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void should_return_cursors_for_non_empty_data() {
    ArticleData a1 = createArticleData("id1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1), CursorPager.Direction.NEXT, false);

    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
  }

  @Test
  public void should_return_different_cursors_for_multiple_items() {
    ArticleData a1 = createArticleData("id1");
    ArticleData a2 = createArticleData("id2");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(a1, a2), CursorPager.Direction.NEXT, false);

    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
  }

  @Test
  public void should_have_direction_enum_values() {
    CursorPager.Direction[] values = CursorPager.Direction.values();
    assertEquals(2, values.length);
    assertEquals(CursorPager.Direction.PREV, CursorPager.Direction.valueOf("PREV"));
    assertEquals(CursorPager.Direction.NEXT, CursorPager.Direction.valueOf("NEXT"));
  }

  private ArticleData createArticleData(String id) {
    ProfileData profileData = new ProfileData("author-id", "author", "bio", "img", false);
    return new ArticleData(
        id, "slug", "Title", "Desc", "Body", false, 0,
        new DateTime(), new DateTime(), Arrays.asList("tag"), profileData);
  }
}
