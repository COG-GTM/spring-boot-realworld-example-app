package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteCountTest {

  @Test
  public void should_expose_article_id_and_count() {
    ArticleFavoriteCount favoriteCount = new ArticleFavoriteCount("article-id", 5);

    assertThat(favoriteCount.getId()).isEqualTo("article-id");
    assertThat(favoriteCount.getCount()).isEqualTo(5);
  }

  @Test
  public void should_be_value_equal_for_same_id_and_count() {
    ArticleFavoriteCount one = new ArticleFavoriteCount("article-id", 5);
    ArticleFavoriteCount other = new ArticleFavoriteCount("article-id", 5);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("article-id");
    assertThat(one).isNotEqualTo(new ArticleFavoriteCount("article-id", 6));
  }

  @Test
  public void should_allow_null_count_for_articles_without_favorites() {
    ArticleFavoriteCount favoriteCount = new ArticleFavoriteCount("article-id", null);

    assertThat(favoriteCount.getCount()).isNull();
  }
}
