package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  @Test
  void should_create_empty_cursor_pager() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertTrue(pager.getData().isEmpty());
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_create_cursor_pager_with_data_next_direction() {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    ArticleData article1 =
        new ArticleData(
            "id1",
            "slug1",
            "Title 1",
            "desc1",
            "body1",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    ArticleData article2 =
        new ArticleData(
            "id2",
            "slug2",
            "Title 2",
            "desc2",
            "body2",
            false,
            0,
            new DateTime(),
            new DateTime().plusHours(1),
            Collections.emptyList(),
            profileData);
    List<ArticleData> articles = Arrays.asList(article1, article2);

    CursorPager<ArticleData> pager = new CursorPager<>(articles, Direction.NEXT, true);

    assertEquals(2, pager.getData().size());
    assertNotNull(pager.getStartCursor());
    assertNotNull(pager.getEndCursor());
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  void should_create_cursor_pager_with_prev_direction() {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    ArticleData article1 =
        new ArticleData(
            "id1",
            "slug1",
            "Title 1",
            "desc1",
            "body1",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    List<ArticleData> articles = Arrays.asList(article1);

    CursorPager<ArticleData> pager = new CursorPager<>(articles, Direction.PREV, true);

    assertEquals(1, pager.getData().size());
    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  void should_handle_cursor_page_parameter() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.NEXT);

    assertEquals(10, param.getLimit());
    assertEquals(Direction.NEXT, param.getDirection());
    assertTrue(param.isNext());
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  void should_handle_cursor_page_parameter_with_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 5, Direction.NEXT);

    assertTrue(param.isNext());
    assertEquals(5, param.getLimit());
  }

  @Test
  void should_parse_datetime_cursor() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);
    String cursorStr = cursor.toString();

    DateTime parsed = DateTimeCursor.parse(cursorStr);
    assertNotNull(parsed);
  }

  @Test
  void should_return_null_for_null_cursor_string() {
    DateTime parsed = DateTimeCursor.parse(null);
    assertNull(parsed);
  }
}
