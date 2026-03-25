package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.DateTimeCursor;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDataCoverageTest {

  @Test
  public void should_create_with_all_args() {
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    DateTime now = new DateTime();
    ArticleData data =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", true, 5, now, now,
            Arrays.asList("tag1"), profile);

    assertEquals("id1", data.getId());
    assertEquals("slug", data.getSlug());
    assertEquals("Title", data.getTitle());
    assertEquals("Desc", data.getDescription());
    assertEquals("Body", data.getBody());
    assertTrue(data.isFavorited());
    assertEquals(5, data.getFavoritesCount());
    assertEquals(now, data.getCreatedAt());
    assertEquals(now, data.getUpdatedAt());
    assertEquals(1, data.getTagList().size());
    assertEquals(profile, data.getProfileData());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    ArticleData data = new ArticleData();
    assertNull(data.getId());
    assertNull(data.getSlug());
    assertFalse(data.isFavorited());
    assertEquals(0, data.getFavoritesCount());
  }

  @Test
  public void should_set_favorited() {
    ArticleData data = new ArticleData();
    data.setFavorited(true);
    assertTrue(data.isFavorited());
  }

  @Test
  public void should_set_favorites_count() {
    ArticleData data = new ArticleData();
    data.setFavoritesCount(10);
    assertEquals(10, data.getFavoritesCount());
  }

  @Test
  public void should_get_cursor() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    ArticleData data =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", false, 0, now, now,
            Arrays.asList("tag1"), profile);

    DateTimeCursor cursor = data.getCursor();
    assertNotNull(cursor);
    assertEquals(now, cursor.getData());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    ArticleData data1 =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", false, 0, now, now,
            Arrays.asList("tag1"), profile);
    ArticleData data2 =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", false, 0, now, now,
            Arrays.asList("tag1"), profile);

    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void should_have_toString() {
    ArticleData data = new ArticleData();
    data.setSlug("test-slug");
    assertNotNull(data.toString());
  }
}
