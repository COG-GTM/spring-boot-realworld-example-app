package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.DateTimeCursor;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDataTest {

  private ArticleData sampleArticle(DateTime createdAt, DateTime updatedAt) {
    return new ArticleData(
        "article-id",
        "a-title",
        "A Title",
        "desc",
        "body",
        true,
        3,
        createdAt,
        updatedAt,
        Arrays.asList("java", "spring"),
        new ProfileData("user-id", "jane", "bio", "image", false));
  }

  @Test
  public void should_expose_all_constructor_values() {
    DateTime createdAt = new DateTime(1000L);
    DateTime updatedAt = new DateTime(2000L);

    ArticleData articleData = sampleArticle(createdAt, updatedAt);

    assertThat(articleData.getId()).isEqualTo("article-id");
    assertThat(articleData.getSlug()).isEqualTo("a-title");
    assertThat(articleData.getTitle()).isEqualTo("A Title");
    assertThat(articleData.getDescription()).isEqualTo("desc");
    assertThat(articleData.getBody()).isEqualTo("body");
    assertThat(articleData.isFavorited()).isTrue();
    assertThat(articleData.getFavoritesCount()).isEqualTo(3);
    assertThat(articleData.getCreatedAt()).isEqualTo(createdAt);
    assertThat(articleData.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(articleData.getTagList()).containsExactly("java", "spring");
    assertThat(articleData.getProfileData().getUsername()).isEqualTo("jane");
  }

  @Test
  public void should_build_cursor_from_updated_at() {
    ArticleData articleData = sampleArticle(new DateTime(1000L), new DateTime(2000L));

    DateTimeCursor cursor = articleData.getCursor();

    assertThat(cursor.getData()).isEqualTo(new DateTime(2000L));
    assertThat(cursor.toString()).isEqualTo("2000");
  }

  @Test
  public void should_reflect_setter_changes_in_cursor() {
    ArticleData articleData = new ArticleData();
    articleData.setUpdatedAt(new DateTime(4242L));
    articleData.setFavorited(true);
    articleData.setFavoritesCount(7);

    assertThat(articleData.getCursor().toString()).isEqualTo("4242");
    assertThat(articleData.isFavorited()).isTrue();
    assertThat(articleData.getFavoritesCount()).isEqualTo(7);
  }

  @Test
  public void should_be_equal_for_same_values() {
    DateTime createdAt = new DateTime(1000L);
    DateTime updatedAt = new DateTime(2000L);

    ArticleData one = sampleArticle(createdAt, updatedAt);
    ArticleData other = sampleArticle(createdAt, updatedAt);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("a-title");

    other.setTitle("Another Title");
    assertThat(one).isNotEqualTo(other);
  }
}
