package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.DateTimeCursor;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ArticleDataTest {

  @Test
  public void should_create_article_data_with_all_args_constructor() {
    DateTime now = new DateTime(DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    List<String> tags = Arrays.asList("java", "spring");
    ArticleData articleData = new ArticleData(
        "id123", "article-slug", "Article Title", "description", "body content",
        true, 5, now, now, tags, profile
    );
    
    assertThat(articleData.getId(), is("id123"));
    assertThat(articleData.getSlug(), is("article-slug"));
    assertThat(articleData.getTitle(), is("Article Title"));
    assertThat(articleData.getDescription(), is("description"));
    assertThat(articleData.getBody(), is("body content"));
    assertThat(articleData.isFavorited(), is(true));
    assertThat(articleData.getFavoritesCount(), is(5));
    assertThat(articleData.getCreatedAt(), is(now));
    assertThat(articleData.getUpdatedAt(), is(now));
    assertThat(articleData.getTagList(), is(tags));
    assertThat(articleData.getProfileData(), is(profile));
  }

  @Test
  public void should_create_empty_article_data_with_no_args_constructor() {
    ArticleData articleData = new ArticleData();
    
    assertThat(articleData.getId(), nullValue());
    assertThat(articleData.getSlug(), nullValue());
    assertThat(articleData.getTitle(), nullValue());
    assertThat(articleData.getDescription(), nullValue());
    assertThat(articleData.getBody(), nullValue());
    assertThat(articleData.isFavorited(), is(false));
    assertThat(articleData.getFavoritesCount(), is(0));
    assertThat(articleData.getCreatedAt(), nullValue());
    assertThat(articleData.getUpdatedAt(), nullValue());
    assertThat(articleData.getTagList(), nullValue());
    assertThat(articleData.getProfileData(), nullValue());
  }

  @Test
  public void should_return_cursor_based_on_updated_at() {
    DateTime createdAt = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTime updatedAt = new DateTime(2023, 6, 16, 10, 30, 0, DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    ArticleData articleData = new ArticleData(
        "id123", "slug", "title", "desc", "body",
        false, 0, createdAt, updatedAt, Arrays.asList("tag"), profile
    );
    
    DateTimeCursor cursor = articleData.getCursor();
    
    assertThat(cursor, notNullValue());
    assertThat(cursor.getData(), is(updatedAt));
  }

  @Test
  public void should_allow_setting_fields() {
    ArticleData articleData = new ArticleData();
    DateTime now = new DateTime(DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    List<String> tags = Arrays.asList("java", "spring");
    
    articleData.setId("id123");
    articleData.setSlug("article-slug");
    articleData.setTitle("Article Title");
    articleData.setDescription("description");
    articleData.setBody("body content");
    articleData.setFavorited(true);
    articleData.setFavoritesCount(5);
    articleData.setCreatedAt(now);
    articleData.setUpdatedAt(now);
    articleData.setTagList(tags);
    articleData.setProfileData(profile);
    
    assertThat(articleData.getId(), is("id123"));
    assertThat(articleData.getSlug(), is("article-slug"));
    assertThat(articleData.getTitle(), is("Article Title"));
    assertThat(articleData.getDescription(), is("description"));
    assertThat(articleData.getBody(), is("body content"));
    assertThat(articleData.isFavorited(), is(true));
    assertThat(articleData.getFavoritesCount(), is(5));
    assertThat(articleData.getCreatedAt(), is(now));
    assertThat(articleData.getUpdatedAt(), is(now));
    assertThat(articleData.getTagList(), is(tags));
    assertThat(articleData.getProfileData(), is(profile));
  }
}
