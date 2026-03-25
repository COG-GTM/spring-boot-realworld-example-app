package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDataListCoverageTest {

  @Test
  public void should_create_with_articles_and_count() {
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    DateTime now = new DateTime();
    ArticleData article =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", false, 0, now, now,
            Arrays.asList("tag1"), profile);

    ArticleDataList list = new ArticleDataList(Arrays.asList(article), 1);

    assertEquals(1, list.getCount());
    assertEquals(1, list.getArticleDatas().size());
    assertEquals("id1", list.getArticleDatas().get(0).getId());
  }

  @Test
  public void should_create_empty_list() {
    ArticleDataList list = new ArticleDataList(Collections.emptyList(), 0);

    assertEquals(0, list.getCount());
    assertTrue(list.getArticleDatas().isEmpty());
  }

  @Test
  public void should_have_count_different_from_list_size() {
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    DateTime now = new DateTime();
    ArticleData article =
        new ArticleData("id1", "slug", "Title", "Desc", "Body", false, 0, now, now,
            Arrays.asList("tag1"), profile);

    ArticleDataList list = new ArticleDataList(Arrays.asList(article), 100);

    assertEquals(1, list.getArticleDatas().size());
    assertEquals(100, list.getCount());
  }
}
