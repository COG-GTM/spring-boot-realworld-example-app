package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.spring.application.DateTimeCursor;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDataTest {

  @Test
  public void should_create_article_data_with_all_fields() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);
    List<String> tags = Arrays.asList("java", "spring");

    ArticleData articleData =
        new ArticleData(
            "article-id",
            "article-slug",
            "Article Title",
            "Description",
            "Body content",
            true,
            10,
            now,
            now,
            tags,
            author);

    assertThat(articleData.getId(), is("article-id"));
    assertThat(articleData.getSlug(), is("article-slug"));
    assertThat(articleData.getTitle(), is("Article Title"));
    assertThat(articleData.getDescription(), is("Description"));
    assertThat(articleData.getBody(), is("Body content"));
    assertThat(articleData.isFavorited(), is(true));
    assertThat(articleData.getFavoritesCount(), is(10));
    assertThat(articleData.getCreatedAt(), is(now));
    assertThat(articleData.getUpdatedAt(), is(now));
    assertThat(articleData.getTagList(), is(tags));
    assertThat(articleData.getProfileData(), is(author));
  }

  @Test
  public void should_create_empty_article_data() {
    ArticleData articleData = new ArticleData();

    assertThat(articleData.getId(), is((String) null));
    assertThat(articleData.getSlug(), is((String) null));
    assertThat(articleData.getTitle(), is((String) null));
    assertThat(articleData.getDescription(), is((String) null));
    assertThat(articleData.getBody(), is((String) null));
    assertThat(articleData.isFavorited(), is(false));
    assertThat(articleData.getFavoritesCount(), is(0));
  }

  @Test
  public void should_get_cursor_from_updated_at() {
    DateTime now = DateTime.now();
    ArticleData articleData = new ArticleData();
    articleData.setUpdatedAt(now);

    DateTimeCursor cursor = articleData.getCursor();

    assertNotNull(cursor);
    assertThat(cursor.getData(), is(now));
  }

  @Test
  public void should_set_article_data_fields() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);
    List<String> tags = Arrays.asList("java", "spring");

    ArticleData articleData = new ArticleData();
    articleData.setId("new-id");
    articleData.setSlug("new-slug");
    articleData.setTitle("New Title");
    articleData.setDescription("New Description");
    articleData.setBody("New Body");
    articleData.setFavorited(true);
    articleData.setFavoritesCount(5);
    articleData.setCreatedAt(now);
    articleData.setUpdatedAt(now);
    articleData.setTagList(tags);
    articleData.setProfileData(author);

    assertThat(articleData.getId(), is("new-id"));
    assertThat(articleData.getSlug(), is("new-slug"));
    assertThat(articleData.getTitle(), is("New Title"));
    assertThat(articleData.getDescription(), is("New Description"));
    assertThat(articleData.getBody(), is("New Body"));
    assertThat(articleData.isFavorited(), is(true));
    assertThat(articleData.getFavoritesCount(), is(5));
    assertThat(articleData.getCreatedAt(), is(now));
    assertThat(articleData.getUpdatedAt(), is(now));
    assertThat(articleData.getTagList(), is(tags));
    assertThat(articleData.getProfileData(), is(author));
  }
}
